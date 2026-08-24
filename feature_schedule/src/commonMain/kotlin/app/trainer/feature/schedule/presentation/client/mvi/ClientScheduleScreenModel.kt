package app.trainer.feature.schedule.presentation.client.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.clients.CoachSummary
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.schedule.ClientSchedule
import app.trainer.data.schedule.ClientSlot
import app.trainer.data.schedule.ScheduleRepository
import app.trainer.data.schedule.SlotChangeKind
import app.trainer.entities.RequestResult
import app.trainer.feature.schedule.domain.ScheduleWeeks
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime

private val WEEKDAY_LABELS = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

private val MONTH_NAMES = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

class ClientScheduleScreenModel(
    private val scheduleRepository: ScheduleRepository,
    private val participantsRepository: ParticipantsRepository,
    private val weeks: ScheduleWeeks,
) : BaseScreenModel<ClientScheduleState, ClientScheduleSideEffect, ClientScheduleEvent>(
    initialState = ClientScheduleState.initial(),
) {

    private var zonesByCoachId: Map<String, TimeZone> = emptyMap()

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
        updateState { it.copy(isLoading = true, isFailed = false) }
        when (val loaded = participantsRepository.coachesOfClient()) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, isFailed = true) }
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

        val options = coaches.map { CoachOption(coachId = it.coachId, displayName = it.displayName) }
        updateState { it.copy(coaches = options.toImmutableList()) }

        val firstCoachId = options.firstOrNull()?.coachId
        if (firstCoachId == null) {
            updateState { it.copy(isLoading = false, isFailed = false) }
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
            updateState { it.copy(isLoading = false, isFailed = true) }
            postSideEffect(
                ClientScheduleSideEffect.ShowFailure(
                    RequestResult.Error(
                        statusCode = null,
                        userMessage = "",
                        devMessage = "Не разобран часовой пояс тренера coachId=$coachId",
                    )
                )
            )
            return
        }

        val targetWeek = weekStart ?: weeks.weekStartOf(weeks.dateOf(Clock.System.now(), zone))
        updateState { it.copy(isLoading = true, isFailed = false, selectedCoachId = coachId) }

        val schedule = scheduleRepository.clientSchedule(
            coachId = coachId,
            from = weeks.startInstant(weekStart = targetWeek, zone = zone),
            to = weeks.endInstant(weekStart = targetWeek, zone = zone),
        )
        when (schedule) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, isFailed = true) }
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
        val selectedDate = state.selectedDate?.takeIf { it in weeks.datesOf(weekStart) } ?: weekStart
        val days = weeks.datesOf(weekStart).map { date ->
            ClientScheduleDay(
                date = date,
                weekdayLabel = WEEKDAY_LABELS[date.dayOfWeek.ordinal],
                dayNumberLabel = date.dayOfMonth.toString(),
                isSelected = date == selectedDate,
                isToday = date == today,
                isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY,
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
        updateState { current ->
            current.copy(
                weekStart = weekStart,
                weekTitle = formatWeekTitle(weekStart = weekStart),
                selectedDate = current.selectedDate.takeIf { date -> days.any { it.date == date } } ?: weekStart,
                days = days.toImmutableList(),
                isLoading = false,
                isFailed = false,
            )
        }
    }

    private fun toRow(slot: ClientSlot, zone: TimeZone, cancellationWindowHours: Int): ClientSlotRow =
        ClientSlotRow(
            slotId = slot.id,
            timeLabel = formatTime(slot.startsAt.toLocalDateTime(zone)),
            durationLabel = "${slot.durationMinutes} мин",
            isBookedByMe = slot.isBookedByMe,
            isAvailable = slot.isAvailable,
            hasPendingChangeRequest = slot.pendingChangeRequestId != null,
            canRequestChange = slot.canRequestChange,
            isOnWaitlist = slot.isOnWaitlist,
            note = changeWindowNote(slot = slot, cancellationWindowHours = cancellationWindowHours),
        )

    private fun changeWindowNote(slot: ClientSlot, cancellationWindowHours: Int): String {
        if (!slot.isBookedByMe || slot.canRequestChange) return ""
        return "Отменить или перенести можно не позже чем за $cancellationWindowHours ч"
    }

    private fun formatTime(dateTime: LocalDateTime): String {
        val hours = dateTime.hour.toString().padStart(length = 2, padChar = '0')
        val minutes = dateTime.minute.toString().padStart(length = 2, padChar = '0')
        return "$hours:$minutes"
    }

    private fun formatWeekTitle(weekStart: LocalDate): String {
        val weekEnd = weeks.datesOf(weekStart).last()
        val month = MONTH_NAMES[weekEnd.monthNumber - 1]
        return "${weekStart.dayOfMonth}—${weekEnd.dayOfMonth} $month"
    }
}
