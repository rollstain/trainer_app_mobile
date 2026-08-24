package app.trainer.feature.schedule.presentation.coach.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.schedule.CoachSchedule
import app.trainer.data.schedule.CoachSlot
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.schedule.ScheduleRepository
import app.trainer.data.schedule.SlotChangeRequest
import app.trainer.entities.RequestResult
import app.trainer.feature.schedule.domain.ScheduleWeeks
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime

private const val MINUTES_IN_HOUR = 60

private val WEEKDAY_LABELS = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

private val MONTH_NAMES = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

class CoachScheduleScreenModel(
    private val scheduleRepository: ScheduleRepository,
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
            CoachScheduleEvent.OnCreateSlotClicked -> openSlotCreation()
            is CoachScheduleEvent.OnDateSelected -> selectDate(event.date)
            is CoachScheduleEvent.OnSlotClicked -> openSlot(event.slotId)
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

    private fun selectDate(date: LocalDate) {
        updateState { it.copy(selectedDate = date) }
    }

    private fun openSlotCreation() {
        screenModelScope {
            postSideEffect(CoachScheduleSideEffect.OpenSlotCreation)
        }
    }

    private fun openSlot(slotId: String) {
        screenModelScope {
            postSideEffect(CoachScheduleSideEffect.OpenSlotDetails(slotId = slotId))
        }
    }

    private fun cancelSlot(slotId: String) {
        screenModelScope {
            when (val cancelled = scheduleRepository.cancelSlot(slotId = slotId)) {
                is RequestResult.Error -> postSideEffect(CoachScheduleSideEffect.ShowFailure(cancelled))
                is RequestResult.Success -> reloadCurrentWeek()
            }
        }
    }

    private fun completeSlot(slotId: String) {
        screenModelScope {
            when (val completed = scheduleRepository.completeSlot(slotId = slotId)) {
                is RequestResult.Error -> postSideEffect(CoachScheduleSideEffect.ShowFailure(completed))
                is RequestResult.Success -> reloadCurrentWeek()
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
                updateState { it.copy(isLoading = false, isFailed = true) }
                postSideEffect(CoachScheduleSideEffect.ShowFailure(profile))
                return null
            }
            is RequestResult.Success -> {
                val zoneId = profile.data.zoneId
                val zone = zoneId?.let(weeks::parseZone)
                if (zone == null) {
                    updateState { it.copy(isLoading = false, isFailed = true) }
                    postSideEffect(
                        CoachScheduleSideEffect.ShowFailure(
                            RequestResult.Error(
                                statusCode = null,
                                userMessage = "",
                                devMessage = "У пользователя нет часового пояса тренера: zoneId=$zoneId",
                            )
                        )
                    )
                    return null
                }
                coachZone = zone
                return zone
            }
        }
    }

    private suspend fun loadWeek(weekStart: LocalDate, zone: TimeZone) {
        updateState { it.copy(isLoading = true, isFailed = false) }

        val schedule = scheduleRepository.coachSchedule(
            from = weeks.startInstant(weekStart = weekStart, zone = zone),
            to = weeks.endInstant(weekStart = weekStart, zone = zone),
        )
        if (schedule is RequestResult.Error) {
            updateState { it.copy(isLoading = false, isFailed = true) }
            postSideEffect(CoachScheduleSideEffect.ShowFailure(schedule))
            return
        }

        val requests = scheduleRepository.pendingChangeRequests()
        if (requests is RequestResult.Error) {
            updateState { it.copy(isLoading = false, isFailed = true) }
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
        val selectedDate = state.selectedDate?.takeIf { it in weeks.datesOf(weekStart) } ?: weekStart

        val days = weeks.datesOf(weekStart).map { date ->
            ScheduleDay(
                date = date,
                weekdayLabel = WEEKDAY_LABELS[date.dayOfWeek.ordinal],
                dayNumberLabel = date.dayOfMonth.toString(),
                isSelected = date == selectedDate,
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
                slotTimeLabel = formatTime(request.slotStartsAt.toLocalDateTime(zone)),
                proposedTimeLabel = request.proposedStartsAt?.let {
                    formatTime(it.toLocalDateTime(zone))
                },
                kind = request.kind,
                requestedByDisplayName = request.requestedByDisplayName,
            )
        }

        updateState { current ->
            current.copy(
                weekStart = weekStart,
                weekTitle = formatWeekTitle(weekStart = weekStart),
                selectedDate = selectedDate,
                days = days.toImmutableList(),
                pendingRequests = requestRows.toImmutableList(),
                isLoading = false,
                isFailed = false,
            )
        }
    }

    private fun toRow(slot: CoachSlot, zone: TimeZone): CoachSlotRow {
        val startsAt = slot.startsAt.toLocalDateTime(zone)
        return CoachSlotRow(
            slotId = slot.id,
            startMinutesOfDay = startsAt.hour * MINUTES_IN_HOUR + startsAt.minute,
            durationMinutes = slot.durationMinutes,
            timeLabel = formatTime(startsAt),
            durationLabel = "${slot.durationMinutes} мин",
            status = slot.status,
            clientDisplayName = slot.clientDisplayName,
            hasPendingChangeRequest = slot.pendingChangeRequestId != null,
        )
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
