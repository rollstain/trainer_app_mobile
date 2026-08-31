package app.trainer.feature.schedule.presentation.newslot.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.TIME_DIGITS_LENGTH
import app.trainer.base.date.filterTimeDigits
import app.trainer.base.date.formatTimeDigits
import app.trainer.base.date.parseTimeDigits
import app.trainer.base.date.weekdayShortOf
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.schedule.CoachScheduleRepository
import app.trainer.data.schedule.SlotSeriesDraft
import app.trainer.entities.RequestResult
import app.trainer.entities.WorkingDay
import app.trainer.feature.schedule.domain.SlotSeriesResults
import app.trainer.feature.schedule.presentation.formatScheduleDate
import app.trainer.feature.schedule.presentation.isOutsideWorkingHours
import app.trainer.strings.Res
import app.trainer.strings.new_slot_outside_schedule
import app.trainer.strings.new_slot_summary_seats
import app.trainer.strings.new_slot_summary_series
import app.trainer.strings.new_slot_summary_single
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.getString

private const val SUMMARY_SEPARATOR = ", "
private const val PERSONAL_CAPACITY = 1

@OptIn(ExperimentalUuidApi::class)
class NewSlotScreenModel(
    initialDateIso: String,
    private val scheduleRepository: CoachScheduleRepository,
    private val clientsRepository: ParticipantsRepository,
    private val profileRepository: ProfileRepository,
    private val weeks: ScheduleWeeks,
    private val seriesResults: SlotSeriesResults,
) : BaseScreenModel<NewSlotState, NewSlotSideEffect, NewSlotEvent>(
    initialState = NewSlotState.initial(date = LocalDate.parse(initialDateIso)),
) {

    private var coachZone: TimeZone? = null
    private var workingHours: List<WorkingDay> = emptyList()

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope { state ->
            coachZone = resolveZone()
            workingHours = loadWorkingHours()
            val dateLabel = formatDate(state.date)
            val weekDays = defaultWeekDays(state.date)
            updateState { current ->
                current.copy(
                    dateLabel = dateLabel,
                    weekDays = weekDays.toImmutableList(),
                )
            }
            refreshSummary()
        }
    }

    private suspend fun loadWorkingHours(): List<WorkingDay> {
        val policy = clientsRepository.coachPolicy()
        if (policy !is RequestResult.Success) return emptyList()
        return policy.data.workingHours
    }

    override fun dispatch(event: NewSlotEvent) {
        when (event) {
            NewSlotEvent.OnSubmitClicked -> submit()
            is NewSlotEvent.OnModeChanged -> updateStateAndSummary { it.copy(mode = event.mode) }
            is NewSlotEvent.OnTimeChanged -> updateStateAndSummary {
                it.copy(timeText = filterTimeDigits(event.text))
            }
            is NewSlotEvent.OnDurationChanged -> updateStateAndSummary {
                it.copy(durationMinutes = event.minutes)
            }
            is NewSlotEvent.OnCapacityChanged -> updateState { it.copy(capacity = event.capacity) }
            is NewSlotEvent.OnWeeksCountChanged -> updateStateAndSummary {
                it.copy(weeksCount = event.weeks)
            }
            is NewSlotEvent.OnWeekDayToggled -> updateStateAndSummary { current ->
                current.copy(
                    weekDays = current.weekDays
                        .map { day ->
                            if (day.dayOfWeek == event.dayOfWeek) {
                                day.copy(isSelected = !day.isSelected)
                            } else {
                                day
                            }
                        }
                        .toImmutableList()
                )
            }
        }
    }

    private fun updateStateAndSummary(change: (NewSlotState) -> NewSlotState) {
        updateState(change)
        screenModelScope { refreshSummary() }
    }

    private suspend fun refreshSummary() {
        val summaryLabel = buildSummary(state)
        val outsideScheduleWarning = outsideScheduleWarning(state)
        updateState { current ->
            current.copy(summaryLabel = summaryLabel, outsideScheduleWarning = outsideScheduleWarning)
        }
    }

    private suspend fun outsideScheduleWarning(state: NewSlotState): String? {
        if (workingHours.isEmpty()) return null
        val time = parseTimeDigits(state.timeText) ?: return null
        val outside = when (state.mode) {
            SlotMode.Single -> isOutsideWorkingHours(
                dayOfWeek = state.date.dayOfWeek,
                time = time,
                workingHours = workingHours,
            )
            SlotMode.Series ->
                state.weekDays
                    .filter { it.isSelected }
                    .any { isOutsideWorkingHours(dayOfWeek = it.dayOfWeek, time = time, workingHours = workingHours) }
        }
        if (!outside) return null
        return getString(Res.string.new_slot_outside_schedule)
    }

    private suspend fun buildSummary(state: NewSlotState): String {
        if (state.timeText.length != TIME_DIGITS_LENGTH) return ""
        val base = baseSummary(state)
        if (state.capacity == PERSONAL_CAPACITY) return base
        return base + SUMMARY_SEPARATOR + getString(Res.string.new_slot_summary_seats, state.capacity)
    }

    private suspend fun baseSummary(state: NewSlotState): String {
        return when (state.mode) {
            SlotMode.Single -> getString(
                Res.string.new_slot_summary_single,
                formatDate(state.date),
                formatTimeDigits(state.timeText),
                state.durationMinutes,
            )
            SlotMode.Series -> {
                val days = state.weekDays.filter { it.isSelected }.joinToString(", ") { it.label }
                val count = state.weekDays.count { it.isSelected } * state.weeksCount
                getString(Res.string.new_slot_summary_series, count, days, formatTimeDigits(state.timeText))
            }
        }
    }

    private fun submit() {
        screenModelScope { state ->
            if (!state.isSubmitEnabled) return@screenModelScope
            val zone = coachZone ?: return@screenModelScope
            val time = parseTimeDigits(state.timeText) ?: return@screenModelScope

            updateState { it.copy(isSubmitting = true) }
            when (state.mode) {
                SlotMode.Single -> createSingle(state = state, time = time, zone = zone)
                SlotMode.Series -> createSeries(state = state, time = time)
            }
            updateState { it.copy(isSubmitting = false) }
        }
    }

    private suspend fun createSingle(state: NewSlotState, time: LocalTime, zone: TimeZone) {
        val created = scheduleRepository.createSlot(
            startsAt = state.date.atTime(time).toInstant(zone),
            durationMinutes = state.durationMinutes,
            capacity = state.capacity,
        )
        when (created) {
            is RequestResult.Error -> postSideEffect(NewSlotSideEffect.ShowFailure(created))
            is RequestResult.Success -> postSideEffect(NewSlotSideEffect.SlotCreated)
        }
    }

    private suspend fun createSeries(state: NewSlotState, time: LocalTime) {
        val created = scheduleRepository.createSlotSeries(
            draft = SlotSeriesDraft(
                startDate = state.date,
                weeksCount = state.weeksCount,
                daysOfWeek = state.weekDays.filter { it.isSelected }.map { it.dayOfWeek }.toSet(),
                timeOfDay = time,
                durationMinutes = state.durationMinutes,
                capacity = state.capacity,
            ),
        )
        when (created) {
            is RequestResult.Error -> postSideEffect(NewSlotSideEffect.ShowFailure(created))
            is RequestResult.Success -> {
                val batchId = Uuid.random().toString()
                seriesResults.put(batchId = batchId, result = created.data)
                postSideEffect(NewSlotSideEffect.SeriesCreated(batchId = batchId))
            }
        }
    }

    private suspend fun resolveZone(): TimeZone? {
        return when (val profile = profileRepository.me()) {
            is RequestResult.Error -> {
                postSideEffect(NewSlotSideEffect.ShowFailure(profile))
                null
            }
            is RequestResult.Success -> profile.data.zoneId?.let(weeks::parseZone)
        }
    }

    private suspend fun defaultWeekDays(date: LocalDate): List<WeekDayToggle> {
        val workingDays = workingHours.map { it.dayOfWeek }.toSet()
        return DayOfWeek.entries.map { dayOfWeek ->
            WeekDayToggle(
                dayOfWeek = dayOfWeek,
                label = weekdayShortOf(dayOfWeek.ordinal),
                isSelected = if (workingDays.isEmpty()) dayOfWeek == date.dayOfWeek else dayOfWeek in workingDays,
            )
        }
    }

    private suspend fun formatDate(date: LocalDate): String = formatScheduleDate(date)
}
