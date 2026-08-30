package app.trainer.feature.schedule.presentation.client.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.formatWorkingSchedule
import app.trainer.base.date.timeOfDayOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.data.clients.CoachSummary
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.schedule.ClientSchedule
import app.trainer.data.schedule.ClientScheduleRepository
import app.trainer.data.schedule.ClientSlot
import app.trainer.data.schedule.SlotChangeKind
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.entities.WorkingDay
import app.trainer.feature.schedule.presentation.formatScheduleWeekTitle
import app.trainer.feature.schedule.presentation.isDayOff
import app.trainer.strings.Res
import app.trainer.strings.client_schedule_cancellation_note
import app.trainer.strings.slot_duration_minutes
import app.trainer.strings.slot_seats_left
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

class ClientScheduleScreenModel(
    private val scheduleRepository: ClientScheduleRepository,
    private val participantsRepository: ParticipantsRepository,
    private val weeks: ScheduleWeeks,
) : BaseScreenModel<ClientScheduleState, ClientScheduleSideEffect, ClientScheduleEvent>(
    initialState = ClientScheduleState.initial(),
) {

    private var zonesByCoachId: Map<String, TimeZone> = emptyMap()
    private var workingHoursByCoachId: Map<String, List<WorkingDay>> = emptyMap()

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            loadCoaches()
        }
    }

    override fun dispatch(event: ClientScheduleEvent) {
        when (event) {
            ClientScheduleEvent.OnRetryClicked -> onFetchData()
            ClientScheduleEvent.OnWriteCoachClicked -> screenModelScope {
                postSideEffect(ClientScheduleSideEffect.OpenChat)
            }
            ClientScheduleEvent.OnPreviousWeekClicked -> shiftWeek(offset = -1)
            ClientScheduleEvent.OnNextWeekClicked -> shiftWeek(offset = 1)
            is ClientScheduleEvent.OnCoachSelected -> selectCoach(event.coachId)
            is ClientScheduleEvent.OnDateSelected -> updateState { it.copy(selectedDate = event.date) }
            is ClientScheduleEvent.OnBookClicked -> bookSlot(event.slotId)
            is ClientScheduleEvent.OnCancelClicked -> updateState {
                it.copy(slotPendingCancel = event.slotId)
            }
            ClientScheduleEvent.OnCancelDismissed -> updateState { it.copy(slotPendingCancel = null) }
            ClientScheduleEvent.OnCancelConfirmed -> confirmCancel()
            is ClientScheduleEvent.OnWaitlistToggled -> toggleWaitlist(event.slotId)
            is ClientScheduleEvent.OnRescheduleRequested -> requestChange(
                slotId = event.slotId,
                kind = SlotChangeKind.RESCHEDULE,
                proposedStartsAt = event.proposedStartsAt,
            )
        }
    }

    private suspend fun loadCoaches() {
        updateState { it.copy(isLoading = true, failure = null) }
        when (val loaded = participantsRepository.coachesOfClient()) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = loaded) }
                postSideEffect(ClientScheduleSideEffect.ShowFailure(loaded))
            }
            is RequestResult.Success -> showCoaches(loaded.data)
        }
    }

    private suspend fun showCoaches(coaches: List<CoachSummary>) {
        zonesByCoachId = coaches.mapNotNull { coach ->
            val zone = weeks.parseZone(coach.zoneId) ?: return@mapNotNull null
            coach.coachId to zone
        }.toMap()
        workingHoursByCoachId = coaches.associate { it.coachId to it.workingHours }

        val options = coaches.map { CoachOption(coachId = it.coachId, displayName = it.displayName) }
        updateState { it.copy(coaches = options.toImmutableList()) }

        val firstCoachId = options.firstOrNull()?.coachId
        if (firstCoachId == null) {
            updateState { it.copy(isLoading = false, failure = null) }
            return
        }
        openCoachWeek(coachId = firstCoachId, weekStart = null)
    }

    private fun selectCoach(coachId: String) {
        screenModelScope {
            openCoachWeek(coachId = coachId, weekStart = null)
        }
    }

    private fun shiftWeek(offset: Int) {
        screenModelScope { state ->
            val coachId = state.selectedCoachId ?: return@screenModelScope
            val current = state.weekStart ?: return@screenModelScope
            openCoachWeek(
                coachId = coachId,
                weekStart = weeks.shiftWeeks(weekStart = current, weeks = offset),
            )
        }
    }

    private fun bookSlot(slotId: String) {
        screenModelScope { state ->
            when (val booked = scheduleRepository.bookSlot(slotId = slotId)) {
                is RequestResult.Error -> postSideEffect(ClientScheduleSideEffect.ShowFailure(booked))
                is RequestResult.Success -> {
                    postSideEffect(ClientScheduleSideEffect.ShowSlotBooked)
                    reload(state)
                }
            }
        }
    }

    private fun confirmCancel() {
        screenModelScope { state ->
            val slotId = state.slotPendingCancel ?: return@screenModelScope
            updateState { it.copy(slotPendingCancel = null) }
            requestChange(slotId = slotId, kind = SlotChangeKind.CANCEL, proposedStartsAt = null)
        }
    }

    private fun toggleWaitlist(slotId: String) {
        screenModelScope { state ->
            val row = state.days
                .flatMap { it.slots }
                .firstOrNull { it.slotId == slotId }
                ?: return@screenModelScope
            val toggled = if (row.isOnWaitlist) {
                scheduleRepository.leaveWaitlist(slotId = slotId)
            } else {
                scheduleRepository.joinWaitlist(slotId = slotId)
            }
            when (toggled) {
                is RequestResult.Error -> postSideEffect(ClientScheduleSideEffect.ShowFailure(toggled))
                is RequestResult.Success -> reload(state)
            }
        }
    }

    private fun requestChange(slotId: String, kind: SlotChangeKind, proposedStartsAt: Instant?) {
        screenModelScope { state ->
            val requested = scheduleRepository.requestChange(
                slotId = slotId,
                kind = kind,
                proposedStartsAt = proposedStartsAt,
            )
            when (requested) {
                is RequestResult.Error -> postSideEffect(ClientScheduleSideEffect.ShowFailure(requested))
                is RequestResult.Success -> {
                    postSideEffect(ClientScheduleSideEffect.ShowChangeRequestSent)
                    reload(state)
                }
            }
        }
    }

    private suspend fun reload(state: ClientScheduleState) {
        val coachId = state.selectedCoachId ?: return
        openCoachWeek(coachId = coachId, weekStart = state.weekStart)
    }

    private suspend fun openCoachWeek(coachId: String, weekStart: LocalDate?) {
        val zone = zonesByCoachId[coachId]
        if (zone == null) {
            val failure = RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "",
                devMessage = "Не разобран часовой пояс тренера coachId=$coachId",
            )
            updateState { it.copy(isLoading = false, failure = failure) }
            postSideEffect(ClientScheduleSideEffect.ShowFailure(failure))
            return
        }

        val targetWeek = weekStart ?: weeks.weekStartOf(weeks.dateOf(Clock.System.now(), zone))
        updateState { it.copy(isLoading = true, failure = null, selectedCoachId = coachId) }

        val schedule = scheduleRepository.clientSchedule(
            coachId = coachId,
            from = weeks.startInstant(weekStart = targetWeek, zone = zone),
            to = weeks.endInstant(weekStart = targetWeek, zone = zone),
        )
        when (schedule) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = schedule) }
                postSideEffect(ClientScheduleSideEffect.ShowFailure(schedule))
            }
            is RequestResult.Success -> showWeek(
                weekStart = targetWeek,
                zone = zone,
                schedule = schedule.data,
            )
        }
    }

    private suspend fun showWeek(weekStart: LocalDate, zone: TimeZone, schedule: ClientSchedule) {
        val today = weeks.dateOf(Clock.System.now(), zone)
        val slotsByDate = schedule.slots.groupBy { slot -> weeks.dateOf(slot.startsAt, zone) }
        val workingHours = state.selectedCoachId?.let { workingHoursByCoachId[it] }.orEmpty()
        val workingDays = workingHours.map { it.dayOfWeek }.toSet()
        val days = weeks.datesOf(weekStart).map { date ->
            ClientScheduleDay(
                date = date,
                weekdayLabel = weekdayShortOf(date),
                dayNumberLabel = date.day.toString(),
                isToday = date == today,
                isDayOff = isDayOff(date = date, workingDays = workingDays),
                slots = slotsByDate[date]
                    .orEmpty()
                    .sortedBy { it.startsAt }
                    .map { slot ->
                        toRow(
                            slot = slot,
                            zone = zone,
                            cancellationWindowHours = schedule.cancellationWindowHours,
                        )
                    }
                    .toImmutableList(),
            )
        }
        val weekTitle = formatWeekTitle(weekStart = weekStart)
        val workingScheduleLabel = formatWorkingSchedule(workingHours)
        updateState { current ->
            current.copy(
                weekStart = weekStart,
                weekTitle = weekTitle,
                workingScheduleLabel = workingScheduleLabel,
                selectedDate = current.selectedDate.takeIf { date -> days.any { it.date == date } } ?: weekStart,
                days = days.toImmutableList(),
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun toRow(slot: ClientSlot, zone: TimeZone, cancellationWindowHours: Int): ClientSlotRow =
        ClientSlotRow(
            slotId = slot.id,
            timeLabel = timeOfDayOf(slot.startsAt.toLocalDateTime(zone)),
            durationLabel = getString(Res.string.slot_duration_minutes, slot.durationMinutes),
            isBookedByMe = slot.isBookedByMe,
            isAvailable = slot.isAvailable,
            hasPendingChangeRequest = slot.pendingChangeRequestId != null,
            canRequestChange = slot.canRequestChange,
            isOnWaitlist = slot.isOnWaitlist,
            note = changeWindowNote(slot = slot, cancellationWindowHours = cancellationWindowHours),
            seatsLabel = seatsLabelOf(slot),
        )

    private suspend fun seatsLabelOf(slot: ClientSlot): String {
        if (!slot.isGroup) return ""
        return getString(Res.string.slot_seats_left, slot.freeSeats, slot.capacity)
    }

    private suspend fun changeWindowNote(slot: ClientSlot, cancellationWindowHours: Int): String {
        if (!slot.isBookedByMe || slot.canRequestChange) return ""
        return getString(Res.string.client_schedule_cancellation_note, cancellationWindowHours)
    }

    private suspend fun formatWeekTitle(weekStart: LocalDate): String =
        formatScheduleWeekTitle(weekStart = weekStart, weekEnd = weeks.datesOf(weekStart).last())
}
