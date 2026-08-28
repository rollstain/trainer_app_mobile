package app.trainer.feature.schedule.presentation.coach.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.timeOfDayOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.schedule.CoachSchedule
import app.trainer.data.schedule.CoachSlot
import app.trainer.data.schedule.ScheduleRepository
import app.trainer.data.schedule.SlotChangeRequest
import app.trainer.data.schedule.SlotStatus
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.feature.schedule.presentation.formatScheduleWeekTitle
import app.trainer.strings.Res
import app.trainer.strings.slot_duration_minutes
import app.trainer.strings.slot_seats_taken
import kotlin.time.Clock
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private const val MINUTES_IN_HOUR = 60
private const val SLOT_TITLE_SEPARATOR = " · "
private const val NAME_SEPARATOR = ", "

class CoachScheduleScreenModel(
    private val scheduleRepository: ScheduleRepository,
    private val clientsRepository: ParticipantsRepository,
    private val profileRepository: ProfileRepository,
    private val weeks: ScheduleWeeks,
) : BaseScreenModel<CoachScheduleState, CoachScheduleSideEffect, CoachScheduleEvent>(
    initialState = CoachScheduleState.initial(),
) {

    private var coachZone: TimeZone? = null

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            val zone = resolveCoachZone() ?: return@onFetchDataScope
            val weekStart = weeks.weekStartOf(weeks.dateOf(Clock.System.now(), zone))
            loadWeek(weekStart = weekStart, zone = zone)
        }
    }

    override fun dispatch(event: CoachScheduleEvent) {
        when (event) {
            CoachScheduleEvent.OnRetryClicked -> onFetchData()
            CoachScheduleEvent.OnPreviousWeekClicked -> shiftWeek(offset = -1)
            CoachScheduleEvent.OnNextWeekClicked -> shiftWeek(offset = 1)
            is CoachScheduleEvent.OnCreateSlotClicked -> openSlotCreation(event.date)
            CoachScheduleEvent.OnSlotCreated -> reloadCurrentWeek()
            is CoachScheduleEvent.OnSlotClicked -> openSlot(event.slotId)
            CoachScheduleEvent.OnSlotActionsDismissed -> updateState { it.copy(slotActions = null) }
            CoachScheduleEvent.OnAssignClientClicked -> openClientPicker()
            is CoachScheduleEvent.OnClientPicked -> assignClient(event.clientUserId)
            CoachScheduleEvent.OnClientPickerDismissed -> updateState {
                it.copy(slotActions = it.slotActions?.copy(picker = null))
            }
            is CoachScheduleEvent.OnParticipantRemoved -> removeParticipant(event.clientUserId)
            is CoachScheduleEvent.OnCancelSlotClicked -> cancelSlot(event.slotId)
            is CoachScheduleEvent.OnCompleteSlotClicked -> completeSlot(event.slotId)
            is CoachScheduleEvent.OnChangeRequestResolved -> resolveRequest(
                requestId = event.requestId,
                approve = event.approve,
            )
        }
    }

    private fun shiftWeek(offset: Int) {
        screenModelScope { state ->
            val zone = coachZone ?: return@screenModelScope
            val current = state.weekStart ?: return@screenModelScope
            loadWeek(weekStart = weeks.shiftWeeks(weekStart = current, weeks = offset), zone = zone)
        }
    }

    private fun openSlotCreation(date: LocalDate?) {
        screenModelScope {
            postSideEffect(CoachScheduleSideEffect.OpenSlotCreation(dateIso = date?.toString()))
        }
    }

    private fun openSlot(slotId: String) {
        screenModelScope { state ->
            val slot = state.days.firstNotNullOfOrNull { day ->
                day.slots.firstOrNull { it.slotId == slotId }
            } ?: return@screenModelScope
            val kind = when (slot.status) {
                SlotStatus.BOOKED -> SlotActionsKind.Booked
                SlotStatus.FREE -> if (slot.hasParticipants) SlotActionsKind.Booked else SlotActionsKind.Free
                SlotStatus.CANCELLED, SlotStatus.COMPLETED -> return@screenModelScope
            }
            updateState { current ->
                current.copy(
                    slotActions = SlotActions(
                        slotId = slot.slotId,
                        title = titleOf(slot),
                        kind = kind,
                        isResolving = false,
                        hasFreeSeats = slot.takenSeats < slot.capacity,
                        participants = slot.participants.toImmutableList(),
                        picker = null,
                    )
                )
            }
        }
    }

    private fun titleOf(slot: CoachSlotRow): String = listOfNotNull(
        slot.timeLabel,
        slot.clientDisplayName,
    ).joinToString(separator = SLOT_TITLE_SEPARATOR)

    private fun openClientPicker() {
        screenModelScope { state ->
            val actions = state.slotActions ?: return@screenModelScope
            updateState {
                it.copy(slotActions = actions.copy(picker = ClientPicker(persistentListOf(), isLoading = true)))
            }
            val taken = actions.participants.map { it.clientUserId }.toSet()
            when (val loaded = clientsRepository.clientsOfCoach()) {
                is RequestResult.Error -> {
                    updateState { it.copy(slotActions = it.slotActions?.copy(picker = null)) }
                    postSideEffect(CoachScheduleSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> {
                    val rows = loaded.data.items
                        .filterNot { client -> taken.contains(client.userId) }
                        .map { client ->
                            ClientPickRow(clientUserId = client.userId, displayName = client.displayName)
                        }
                    updateState { current ->
                        current.copy(
                            slotActions = current.slotActions?.copy(
                                picker = ClientPicker(clients = rows.toImmutableList(), isLoading = false),
                            )
                        )
                    }
                }
            }
        }
    }

    private fun assignClient(clientUserId: String) {
        screenModelScope { state ->
            val slotId = state.slotActions?.slotId ?: return@screenModelScope
            updateState { it.copy(slotActions = it.slotActions?.copy(picker = null)) }
            resolveSlot(slotId = slotId) {
                scheduleRepository.assignSlot(slotId = it, clientUserId = clientUserId)
            }
        }
    }

    private fun removeParticipant(clientUserId: String) {
        screenModelScope { state ->
            val slotId = state.slotActions?.slotId ?: return@screenModelScope
            resolveSlot(slotId = slotId) {
                scheduleRepository.removeParticipant(slotId = it, clientUserId = clientUserId)
            }
        }
    }

    private fun cancelSlot(slotId: String) {
        resolveSlot(slotId = slotId) { scheduleRepository.cancelSlot(slotId = it) }
    }

    private fun completeSlot(slotId: String) {
        resolveSlot(slotId = slotId) { scheduleRepository.completeSlot(slotId = it) }
    }

    private fun resolveSlot(slotId: String, call: suspend (String) -> RequestResult<CoachSlot>) {
        screenModelScope {
            updateState { it.copy(slotActions = it.slotActions?.copy(isResolving = true)) }
            when (val resolved = call(slotId)) {
                is RequestResult.Error -> {
                    updateState { it.copy(slotActions = it.slotActions?.copy(isResolving = false)) }
                    postSideEffect(CoachScheduleSideEffect.ShowFailure(resolved))
                }
                is RequestResult.Success -> {
                    updateState { it.copy(slotActions = null) }
                    reloadCurrentWeek()
                }
            }
        }
    }

    private fun resolveRequest(requestId: String, approve: Boolean) {
        screenModelScope {
            val resolved = scheduleRepository.resolveChangeRequest(requestId = requestId, approve = approve)
            when (resolved) {
                is RequestResult.Error -> postSideEffect(CoachScheduleSideEffect.ShowFailure(resolved))
                is RequestResult.Success -> reloadCurrentWeek()
            }
        }
    }

    private fun reloadCurrentWeek() {
        screenModelScope { state ->
            val zone = coachZone ?: return@screenModelScope
            val weekStart = state.weekStart ?: return@screenModelScope
            loadWeek(weekStart = weekStart, zone = zone)
        }
    }

    private suspend fun resolveCoachZone(): TimeZone? {
        val known = coachZone
        if (known != null) return known

        when (val profile = profileRepository.me()) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = profile) }
                postSideEffect(CoachScheduleSideEffect.ShowFailure(profile))
                return null
            }
            is RequestResult.Success -> {
                val zoneId = profile.data.zoneId
                val zone = zoneId?.let(weeks::parseZone)
                if (zone == null) {
                    val failure = RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "",
                        devMessage = "У пользователя нет часового пояса тренера: zoneId=$zoneId",
                    )
                    updateState { it.copy(isLoading = false, failure = failure) }
                    postSideEffect(CoachScheduleSideEffect.ShowFailure(failure))
                    return null
                }
                coachZone = zone
                return zone
            }
        }
    }

    private suspend fun loadWeek(weekStart: LocalDate, zone: TimeZone) {
        updateState { it.copy(isLoading = true, failure = null) }

        val schedule = scheduleRepository.coachSchedule(
            from = weeks.startInstant(weekStart = weekStart, zone = zone),
            to = weeks.endInstant(weekStart = weekStart, zone = zone),
        )
        if (schedule is RequestResult.Error) {
            updateState { it.copy(isLoading = false, failure = schedule) }
            postSideEffect(CoachScheduleSideEffect.ShowFailure(schedule))
            return
        }

        val requests = scheduleRepository.pendingChangeRequests()
        if (requests is RequestResult.Error) {
            updateState { it.copy(isLoading = false, failure = requests) }
            postSideEffect(CoachScheduleSideEffect.ShowFailure(requests))
            return
        }

        val loadedSchedule = (schedule as RequestResult.Success).data
        val loadedRequests = (requests as RequestResult.Success).data
        showWeek(
            weekStart = weekStart,
            zone = zone,
            schedule = loadedSchedule,
            requests = loadedRequests,
        )
    }

    private suspend fun showWeek(
        weekStart: LocalDate,
        zone: TimeZone,
        schedule: CoachSchedule,
        requests: List<SlotChangeRequest>,
    ) {
        val today = weeks.dateOf(Clock.System.now(), zone)
        val slotsByDate = schedule.slots.groupBy { slot -> weeks.dateOf(slot.startsAt, zone) }
        val days = weeks.datesOf(weekStart).map { date ->
            ScheduleDay(
                date = date,
                weekdayLabel = weekdayShortOf(date),
                dayNumberLabel = date.day.toString(),
                isToday = date == today,
                isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY,
                slots = slotsByDate[date]
                    .orEmpty()
                    .sortedBy { it.startsAt }
                    .map { slot -> toRow(slot = slot, zone = zone) }
                    .toImmutableList(),
            )
        }
        val requestRows = requests.map { request ->
            ChangeRequestRow(
                requestId = request.id,
                slotTimeLabel = timeOfDayOf(request.slotStartsAt.toLocalDateTime(zone)),
                proposedTimeLabel = request.proposedStartsAt?.let {
                    timeOfDayOf(it.toLocalDateTime(zone))
                },
                kind = request.kind,
                requestedByDisplayName = request.requestedByDisplayName,
            )
        }

        val weekTitle = formatWeekTitle(weekStart = weekStart)
        val nextSlotId = nextSlotIdOf(days = days, today = today, zone = zone)
        updateState { current ->
            current.copy(
                weekStart = weekStart,
                weekTitle = weekTitle,
                days = days.toImmutableList(),
                pendingRequests = requestRows.toImmutableList(),
                nextSlotId = nextSlotId,
                isLoading = false,
                failure = null,
            )
        }
    }

    private fun nextSlotIdOf(days: List<ScheduleDay>, today: LocalDate, zone: TimeZone): String? {
        val currentTime = Clock.System.now().toLocalDateTime(zone).time
        val minutesNow = currentTime.hour * MINUTES_IN_HOUR + currentTime.minute
        return days
            .firstOrNull { it.date == today }
            ?.slots
            ?.filter { it.hasParticipants && it.startMinutesOfDay >= minutesNow }
            ?.minByOrNull { it.startMinutesOfDay }
            ?.slotId
    }

    private suspend fun toRow(slot: CoachSlot, zone: TimeZone): CoachSlotRow {
        val startsAt = slot.startsAt.toLocalDateTime(zone)
        return CoachSlotRow(
            slotId = slot.id,
            startMinutesOfDay = startsAt.hour * MINUTES_IN_HOUR + startsAt.minute,
            durationMinutes = slot.durationMinutes,
            timeLabel = timeOfDayOf(startsAt),
            durationLabel = getString(Res.string.slot_duration_minutes, slot.durationMinutes),
            status = slot.status,
            clientDisplayName = slot.clientDisplayName,
            hasPendingChangeRequest = slot.pendingChangeRequestId != null,
            isGroup = slot.isGroup,
            hasParticipants = slot.takenSeats > 0,
            seatsLabel = getString(Res.string.slot_seats_taken, slot.takenSeats, slot.capacity),
            participantNames = slot.participants.mapNotNull { it.displayName }.joinToString(NAME_SEPARATOR),
            capacity = slot.capacity,
            takenSeats = slot.takenSeats,
            participants = slot.participants
                .map {
                    SlotParticipantRow(
                        clientUserId = it.userId,
                        displayName = it.displayName.orEmpty(),
                    )
                }
                .toImmutableList(),
        )
    }

    private suspend fun formatWeekTitle(weekStart: LocalDate): String =
        formatScheduleWeekTitle(weekStart = weekStart, weekEnd = weeks.datesOf(weekStart).last())
}
