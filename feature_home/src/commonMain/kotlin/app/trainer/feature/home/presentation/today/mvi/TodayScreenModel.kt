package app.trainer.feature.home.presentation.today.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.dayMonthOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.base.diary.DiaryLapse
import app.trainer.base.diary.diaryLapseOf
import app.trainer.data.chat.ChatRepository
import app.trainer.data.chat.Dialog
import app.trainer.data.clients.CoachClient
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.progress.AwaitingCheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.schedule.CoachSlot
import app.trainer.data.schedule.ScheduleRepository
import app.trainer.data.schedule.SlotChangeRequest
import app.trainer.data.schedule.SlotStatus
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.feature.home.presentation.startsInLabelOf
import app.trainer.strings.Res
import app.trainer.strings.home_date
import app.trainer.strings.slot_duration_minutes
import app.trainer.strings.today_free_slots_summary
import app.trainer.strings.today_tomorrow_summary
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private const val UNREAD_PREVIEW_COUNT = 3
private const val DIARY_HISTORY_DAYS = 90

private data class TodaySources(
    val coachDisplayName: String,
    val today: LocalDate,
    val zone: TimeZone,
    val slots: List<CoachSlot>,
    val requests: List<SlotChangeRequest>,
    val dialogs: List<Dialog>,
    val lapsed: List<TodayLapsedRow>,
    val awaiting: List<AwaitingCheckIn>,
    val hasClients: Boolean,
)

class TodayScreenModel(
    private val scheduleRepository: ScheduleRepository,
    private val chatRepository: ChatRepository,
    private val participantsRepository: ParticipantsRepository,
    private val trainingLogRepository: TrainingLogRepository,
    private val profileRepository: ProfileRepository,
    private val checkInRepository: CheckInRepository,
    private val weeks: ScheduleWeeks,
) : BaseScreenModel<TodayState, TodaySideEffect, TodayEvent>(
    initialState = TodayState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope { load() }
    }

    override fun dispatch(event: TodayEvent) {
        when (event) {
            TodayEvent.OnRetryClicked -> onFetchData()
            TodayEvent.OnProfileClicked -> post(TodaySideEffect.OpenProfile)
            TodayEvent.OnCalendarClicked -> post(TodaySideEffect.OpenCalendar)
            TodayEvent.OnAllDialogsClicked -> post(TodaySideEffect.OpenChats)
            TodayEvent.OnAddSlotClicked -> post(TodaySideEffect.OpenSlotCreation)
            is TodayEvent.OnSessionClicked -> post(TodaySideEffect.OpenClientCard(event.clientUserId))
            is TodayEvent.OnDialogClicked -> post(TodaySideEffect.OpenDialog(event.dialogId))
            is TodayEvent.OnLapsedClicked -> post(TodaySideEffect.OpenDiary(event.userId))
            is TodayEvent.OnCheckInClicked -> post(TodaySideEffect.OpenClientCard(event.clientUserId))
            is TodayEvent.OnRequestResolved -> resolveRequest(
                requestId = event.requestId,
                approve = event.approve,
            )
        }
    }

    private fun post(effect: TodaySideEffect) {
        screenModelScope { postSideEffect(effect) }
    }

    private fun resolveRequest(requestId: String, approve: Boolean) {
        screenModelScope {
            when (val resolved = scheduleRepository.resolveChangeRequest(requestId, approve)) {
                is RequestResult.Error -> postSideEffect(TodaySideEffect.ShowFailure(resolved))
                is RequestResult.Success -> load()
            }
        }
    }

    private suspend fun load() {
        updateState { it.copy(isLoading = true, failure = null) }

        val profile = profileRepository.me()
        if (profile is RequestResult.Error) return showFailure(profile)
        val coach = (profile as RequestResult.Success).data
        val zone = coach.zoneId?.let(weeks::parseZone)
        if (zone == null) return showFailure(missingZoneFailure(coach.zoneId))

        val today = weeks.dateOf(Clock.System.now(), zone)
        val weekStart = weeks.weekStartOf(today)

        val schedule = scheduleRepository.coachSchedule(
            from = weeks.startInstant(weekStart = weekStart, zone = zone),
            to = weeks.endInstant(weekStart = weekStart, zone = zone),
        )
        if (schedule is RequestResult.Error) return showFailure(schedule)

        val requests = scheduleRepository.pendingChangeRequests()
        if (requests is RequestResult.Error) return showFailure(requests)

        val clients = participantsRepository.clientsOfCoach()
        if (clients is RequestResult.Error) return showFailure(clients)

        val slots = (schedule as RequestResult.Success).data.slots
        val roster = (clients as RequestResult.Success).data.items
        val dialogs = chatRepository.observeDialogs().first()

        show(
            TodaySources(
                coachDisplayName = coach.displayName,
                today = today,
                zone = zone,
                slots = slots,
                requests = (requests as RequestResult.Success).data,
                dialogs = dialogs,
                lapsed = lapsedRowsOf(clients = roster, today = today, zone = zone),
                awaiting = awaitingCheckInsOf(),
                hasClients = roster.isNotEmpty(),
            )
        )
    }

    private suspend fun show(sources: TodaySources) {
        val zone = sources.zone
        val today = sources.today
        val slots = sources.slots
        val now = Clock.System.now()
        val todaySlots = slots.filter { weeks.dateOf(it.startsAt, zone) == today }
        val upcomingBooked = slots
            .filter { it.status == SlotStatus.BOOKED && it.startsAt >= now }
            .sortedBy { it.startsAt }
        val nextTodaySlotId = upcomingBooked
            .firstOrNull { weeks.dateOf(it.startsAt, zone) == today }
            ?.id

        val sessions = todaySlots
            .filter { it.status == SlotStatus.BOOKED }
            .sortedBy { it.startsAt }
            .map { slot -> sessionRowOf(slot = slot, zone = zone, isNext = slot.id == nextTodaySlotId, now = now) }

        val unreadDialogs = sources.dialogs.filter { it.unreadCount > 0 }.sortedByDescending { it.lastMessageAt }
        val tomorrowSlots = slots
            .filter { weeks.dateOf(it.startsAt, zone) == today.plus(DatePeriod(days = 1)) }
            .filter { it.status == SlotStatus.BOOKED }
            .sortedBy { it.startsAt }
        val freeSlots = slots.filter { it.status == SlotStatus.FREE && it.startsAt >= now }

        val requestRows = requestRowsOf(requests = sources.requests, zone = zone)
        val awaitingRows = awaitingRowsOf(sources.awaiting)
        val dateLabel = getString(
            Res.string.home_date,
            weekdayShortOf(today).lowercase(),
            dayMonthOf(today),
        )
        val tomorrow = tomorrowOf(tomorrowSlots = tomorrowSlots, zone = zone)
        val nextSession = nextSessionOf(slot = upcomingBooked.firstOrNull(), zone = zone, now = now)
        val freeSlotsOffer = freeSlotsOf(freeSlots)

        updateState { current ->
            current.copy(
                dateLabel = dateLabel,
                coachDisplayName = sources.coachDisplayName,
                requests = requestRows.toImmutableList(),
                sessions = sessions.toImmutableList(),
                unread = unreadDialogs.take(UNREAD_PREVIEW_COUNT).map(::dialogRowOf).toImmutableList(),
                moreUnreadCount = (unreadDialogs.size - UNREAD_PREVIEW_COUNT).coerceAtLeast(0),
                lapsed = sources.lapsed.toImmutableList(),
                awaitingCheckIns = awaitingRows.toImmutableList(),
                tomorrow = tomorrow,
                nextSession = nextSession,
                freeSlots = freeSlotsOffer,
                hasClients = sources.hasClients,
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun requestRowsOf(requests: List<SlotChangeRequest>, zone: TimeZone): List<TodayRequestRow> =
        requests.map { request ->
            TodayRequestRow(
                requestId = request.id,
                timeLabel = timeOfDayOf(request.slotStartsAt.toLocalDateTime(zone)),
                proposedTimeLabel = request.proposedStartsAt?.let { timeOfDayOf(it.toLocalDateTime(zone)) },
                requestedByDisplayName = request.requestedByDisplayName,
                kind = request.kind,
            )
        }

    private fun awaitingRowsOf(awaiting: List<AwaitingCheckIn>): List<TodayCheckInRow> = awaiting.map { checkIn ->
        TodayCheckInRow(
            checkInId = checkIn.checkInId,
            clientUserId = checkIn.clientUserId,
            displayName = checkIn.clientDisplayName,
            dateLabel = dayMonthOf(checkIn.checkInDate),
        )
    }

    private suspend fun tomorrowOf(tomorrowSlots: List<CoachSlot>, zone: TimeZone): TodayTomorrow = when {
        tomorrowSlots.isEmpty() -> TodayTomorrow.None
        else -> TodayTomorrow.Sessions(
            summary = getString(
                Res.string.today_tomorrow_summary,
                tomorrowSlots.size,
                tomorrowSlots.joinToString(separator = " · ") {
                    timeOfDayOf(it.startsAt.toLocalDateTime(zone))
                },
            )
        )
    }

    private suspend fun freeSlotsOf(freeSlots: List<CoachSlot>): TodayFreeSlots = when {
        freeSlots.isEmpty() -> TodayFreeSlots.None
        else -> TodayFreeSlots.Available(
            summary = getString(Res.string.today_free_slots_summary, freeSlots.size)
        )
    }

    private suspend fun sessionRowOf(
        slot: CoachSlot,
        zone: TimeZone,
        isNext: Boolean,
        now: Instant,
    ): TodaySessionRow = TodaySessionRow(
        slotId = slot.id,
        clientUserId = slot.clientUserId.orEmpty(),
        timeLabel = timeOfDayOf(slot.startsAt.toLocalDateTime(zone)),
        durationLabel = getString(Res.string.slot_duration_minutes, slot.durationMinutes),
        clientDisplayName = slot.clientDisplayName.orEmpty(),
        isNext = isNext,
        startsInLabel = if (isNext) startsInLabelOf(startsAt = slot.startsAt, now = now) else "",
    )

    private fun dialogRowOf(dialog: Dialog): TodayDialogRow = TodayDialogRow(
        dialogId = dialog.id,
        peerDisplayName = dialog.peerDisplayName,
        preview = dialog.lastMessagePreview.orEmpty(),
        unreadCount = dialog.unreadCount,
    )

    private suspend fun nextSessionOf(
        slot: CoachSlot?,
        zone: TimeZone,
        now: Instant,
    ): TodayNextSession {
        if (slot == null) return TodayNextSession.NoneThisWeek
        val date = weeks.dateOf(slot.startsAt, zone)
        return TodayNextSession.Upcoming(
            dayLabel = getString(
                Res.string.home_date,
                weekdayShortOf(date).lowercase(),
                dayMonthOf(date),
            ),
            timeLabel = timeOfDayOf(slot.startsAt.toLocalDateTime(zone)),
            clientDisplayName = slot.clientDisplayName.orEmpty(),
            startsInLabel = startsInLabelOf(startsAt = slot.startsAt, now = now),
        )
    }

    private suspend fun awaitingCheckInsOf(): List<AwaitingCheckIn> {
        return when (val awaiting = checkInRepository.awaitingReview()) {
            is RequestResult.Success -> awaiting.data
            is RequestResult.Error -> emptyList()
        }
    }

    private fun lapsedRow(client: CoachClient, since: LapsedSince): TodayLapsedRow = TodayLapsedRow(
        userId = client.userId,
        displayName = client.displayName,
        since = since,
    )

    private suspend fun lapsedRowsOf(
        clients: List<CoachClient>,
        today: LocalDate,
        zone: TimeZone,
    ): List<TodayLapsedRow> {
        val historyStart = today.minus(DatePeriod(days = DIARY_HISTORY_DAYS - 1))
        return coroutineScope {
            clients
                .map { client ->
                    async {
                        val entries = trainingLogRepository.clientEntries(
                            clientUserId = client.userId,
                            from = historyStart,
                            to = today,
                        )
                        val lastEntry = when (entries) {
                            is RequestResult.Success -> entries.data.maxOfOrNull { it.entryDate }
                            is RequestResult.Error -> null
                        }
                        val lapse = diaryLapseOf(
                            today = today,
                            lastEntryDate = lastEntry,
                            linkedDate = client.linkedAt?.toLocalDateTime(zone)?.date,
                        )
                        when (lapse) {
                            is DiaryLapse.Lapsed -> lapsedRow(client, LapsedSince.Days(lapse.days))
                            DiaryLapse.NeverLogged -> lapsedRow(client, LapsedSince.Never)
                            DiaryLapse.Logging, DiaryLapse.NotStartedYet -> null
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
                .sortedByDescending { daysOf(it.since) }
        }
    }

    private fun daysOf(since: LapsedSince): Int = when (since) {
        LapsedSince.Never -> DIARY_HISTORY_DAYS
        is LapsedSince.Days -> since.value
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        updateState { it.copy(isLoading = false, failure = failure) }
        postSideEffect(TodaySideEffect.ShowFailure(failure))
    }

    private fun missingZoneFailure(zoneId: String?): RequestResult.Error = RequestResult.Error(
        kind = RequestFailure.Parsing,
        statusCode = null,
        userMessage = "",
        devMessage = "У пользователя нет часового пояса тренера: zoneId=$zoneId",
    )
}
