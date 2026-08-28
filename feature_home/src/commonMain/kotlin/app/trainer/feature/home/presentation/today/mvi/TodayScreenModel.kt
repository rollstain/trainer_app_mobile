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
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.progress.AwaitingCheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.FormCheck
import app.trainer.data.progress.FormCheckRepository
import app.trainer.data.schedule.CoachScheduleRepository
import app.trainer.data.schedule.CoachSlot
import app.trainer.data.schedule.SlotChangeRequest
import app.trainer.data.schedule.SlotStatus
import app.trainer.data.traininglog.ClientDiarySummary
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.feature.home.presentation.startsInLabelOf
import app.trainer.strings.Res
import app.trainer.strings.home_date
import app.trainer.strings.slot_duration_minutes
import app.trainer.strings.slot_seats_taken
import app.trainer.strings.today_free_slots_summary
import app.trainer.strings.today_tomorrow_summary
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private const val UNREAD_PREVIEW_COUNT = 3
private const val NAME_SEPARATOR = ", "
private const val DIARY_HISTORY_DAYS = 90

private data class TodaySources(
    val coachDisplayName: String,
    val today: LocalDate,
    val zone: TimeZone,
    val slots: List<CoachSlot>,
    val requests: List<SlotChangeRequest>,
    val dialogs: List<Dialog>,
    val lapsed: RequestResult<List<TodayLapsedRow>>,
    val awaiting: RequestResult<List<AwaitingCheckIn>>,
    val awaitingFormChecks: RequestResult<List<FormCheck>>,
    val hasClients: Boolean,
)

class TodayScreenModel(
    private val scheduleRepository: CoachScheduleRepository,
    private val chatRepository: ChatRepository,
    private val participantsRepository: ParticipantsRepository,
    private val trainingLogRepository: TrainingLogRepository,
    private val profileRepository: ProfileRepository,
    private val checkInRepository: CheckInRepository,
    private val formCheckRepository: FormCheckRepository,
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
            is TodayEvent.OnBlockRetryClicked -> screenModelScope { load(showsShimmer = false) }
            TodayEvent.OnProfileClicked -> post(TodaySideEffect.OpenProfile)
            TodayEvent.OnCalendarClicked -> post(TodaySideEffect.OpenCalendar)
            TodayEvent.OnAllDialogsClicked -> post(TodaySideEffect.OpenChats)
            TodayEvent.OnAddSlotClicked -> post(TodaySideEffect.OpenSlotCreation)
            is TodayEvent.OnSessionClicked -> post(TodaySideEffect.OpenClientCard(event.clientUserId))
            is TodayEvent.OnDialogClicked -> post(TodaySideEffect.OpenDialog(event.dialogId))
            is TodayEvent.OnLapsedClicked -> post(TodaySideEffect.OpenDiary(event.userId))
            is TodayEvent.OnCheckInClicked -> post(TodaySideEffect.OpenClientCard(event.clientUserId))
            TodayEvent.OnFormChecksClicked -> post(TodaySideEffect.OpenFormChecks)
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

    private suspend fun load(showsShimmer: Boolean = true) {
        updateState { it.copy(isLoading = showsShimmer, failure = null) }

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
                lapsed = lapsedRowsOf(today = today, zone = zone),
                awaiting = checkInRepository.awaitingReview(),
                awaitingFormChecks = formCheckRepository.awaitingReview(),
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
            .filter { it.takenSeats > 0 && it.status != SlotStatus.CANCELLED && it.startsAt >= now }
            .sortedBy { it.startsAt }
        val nextTodaySlotId = upcomingBooked
            .firstOrNull { weeks.dateOf(it.startsAt, zone) == today }
            ?.id

        val sessions = todaySlots
            .filter { it.takenSeats > 0 && it.status != SlotStatus.CANCELLED }
            .sortedBy { it.startsAt }
            .map { slot -> sessionRowOf(slot = slot, zone = zone, isNext = slot.id == nextTodaySlotId, now = now) }

        val unreadDialogs = sources.dialogs.filter { it.unreadCount > 0 }.sortedByDescending { it.lastMessageAt }
        val tomorrowSlots = slots
            .filter { weeks.dateOf(it.startsAt, zone) == today.plus(DatePeriod(days = 1)) }
            .filter { it.takenSeats > 0 && it.status != SlotStatus.CANCELLED }
            .sortedBy { it.startsAt }
        val freeSlots = slots.filter { it.status == SlotStatus.FREE && it.startsAt >= now }

        val requestRows = requestRowsOf(requests = sources.requests, zone = zone)
        val awaitingRows = awaitingRowsOf(sources.awaiting.itemsOrEmpty())
        val formCheckRows = formCheckRowsOf(sources.awaitingFormChecks.itemsOrEmpty())
        val failedBlocks = buildSet {
            if (sources.awaiting is RequestResult.Error) add(TodayBlock.CheckIns)
            if (sources.awaitingFormChecks is RequestResult.Error) add(TodayBlock.FormChecks)
            if (sources.lapsed is RequestResult.Error) add(TodayBlock.Lapsed)
        }
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
                lapsed = sources.lapsed.itemsOrEmpty().toImmutableList(),
                awaitingCheckIns = awaitingRows.toImmutableList(),
                awaitingFormChecks = formCheckRows.toImmutableList(),
                tomorrow = tomorrow,
                nextSession = nextSession,
                freeSlots = freeSlotsOffer,
                failedBlocks = failedBlocks.toImmutableSet(),
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

    private fun formCheckRowsOf(awaiting: List<FormCheck>): List<TodayFormCheckRow> = awaiting.map { formCheck ->
        TodayFormCheckRow(
            formCheckId = formCheck.id,
            clientUserId = formCheck.clientUserId,
            displayName = formCheck.clientDisplayName,
            dateLabel = dayMonthOf(formCheck.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date),
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
        clientDisplayName = participantsLabelOf(slot),
        isNext = isNext,
        startsInLabel = if (isNext) startsInLabelOf(startsAt = slot.startsAt, now = now) else "",
        seatsLabel = if (slot.isGroup) {
            getString(Res.string.slot_seats_taken, slot.takenSeats, slot.capacity)
        } else {
            ""
        },
    )

    private fun participantsLabelOf(slot: CoachSlot): String {
        if (!slot.isGroup) return slot.clientDisplayName.orEmpty()
        return slot.participants.mapNotNull { it.displayName }.joinToString(NAME_SEPARATOR)
    }

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
            clientDisplayName = participantsLabelOf(slot),
            startsInLabel = startsInLabelOf(startsAt = slot.startsAt, now = now),
        )
    }

    private fun lapsedRow(client: ClientDiarySummary, since: LapsedSince): TodayLapsedRow = TodayLapsedRow(
        userId = client.clientUserId,
        displayName = client.displayName,
        since = since,
    )

    private suspend fun lapsedRowsOf(
        today: LocalDate,
        zone: TimeZone,
    ): RequestResult<List<TodayLapsedRow>> {
        val historyStart = today.minus(DatePeriod(days = DIARY_HISTORY_DAYS - 1))
        val summary = trainingLogRepository.coachDiarySummary(from = historyStart, to = today)
        if (summary is RequestResult.Error) return summary
        val rows = (summary as RequestResult.Success).data
            .mapNotNull { client ->
                val lapse = diaryLapseOf(
                    today = today,
                    lastEntryDate = client.lastEntryDate,
                    linkedDate = client.linkedAt?.toLocalDateTime(zone)?.date,
                )
                when (lapse) {
                    is DiaryLapse.Lapsed -> lapsedRow(client, LapsedSince.Days(lapse.days))
                    DiaryLapse.NeverLogged -> lapsedRow(client, LapsedSince.Never)
                    DiaryLapse.Logging, DiaryLapse.NotStartedYet -> null
                }
            }
            .sortedByDescending { daysOf(it.since) }
        return RequestResult.Success(rows)
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

private fun <T> RequestResult<List<T>>.itemsOrEmpty(): List<T> = when (this) {
    is RequestResult.Success -> data
    is RequestResult.Error -> emptyList()
}
