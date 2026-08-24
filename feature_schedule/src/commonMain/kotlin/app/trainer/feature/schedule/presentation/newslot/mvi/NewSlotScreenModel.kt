package app.trainer.feature.schedule.presentation.newslot.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.schedule.ScheduleRepository
import app.trainer.data.schedule.SlotSeriesDraft
import app.trainer.entities.RequestResult
import app.trainer.feature.schedule.domain.ScheduleWeeks
import app.trainer.feature.schedule.domain.SlotSeriesResults
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

private const val TIME_SEPARATOR = ':'
private const val MINUTES_IN_HOUR = 60
private val WEEKDAY_LABELS = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
private val MONTH_NAMES = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

@OptIn(ExperimentalUuidApi::class)
class NewSlotScreenModel(
    initialDateIso: String,
    private val scheduleRepository: ScheduleRepository,
    private val profileRepository: ProfileRepository,
    private val weeks: ScheduleWeeks,
    private val seriesResults: SlotSeriesResults,
) : BaseScreenModel<NewSlotState, NewSlotSideEffect, NewSlotEvent>(
    initialState = NewSlotState.initial(date = LocalDate.parse(initialDateIso)),
) {

    private var coachZone: TimeZone? = null

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope { state ->
            coachZone = resolveZone()
            updateState { current ->
                current.copy(
                    dateLabel = formatDate(state.date),
                    weekDays = defaultWeekDays(state.date).toImmutableList(),
                )
            }
            refreshSummary()
        }
    }

    override fun dispatch(event: NewSlotEvent) {
        when (event) {
            NewSlotEvent.OnSubmitClicked -> submit()
            is NewSlotEvent.OnModeChanged -> updateStateAndSummary { it.copy(mode = event.mode) }
            is NewSlotEvent.OnTimeChanged -> updateStateAndSummary {
                it.copy(timeText = normalizeTime(event.text))
            }
            is NewSlotEvent.OnDurationChanged -> updateStateAndSummary {
                it.copy(durationMinutes = event.minutes)
            }
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
        updateState { current -> current.copy(summaryLabel = buildSummary(current)) }
    }

    private fun buildSummary(state: NewSlotState): String {
        if (state.timeText.length != NewSlotState.TIME_LENGTH) return ""
        return when (state.mode) {
            SlotMode.Single -> "${formatDate(state.date)}, ${state.timeText}, ${state.durationMinutes} мин"
            SlotMode.Series -> {
                val days = state.weekDays.filter { it.isSelected }.joinToString(", ") { it.label }
                val count = state.weekDays.count { it.isSelected } * state.weeksCount
                "$count слотов: $days в ${state.timeText}"
            }
        }
    }

    private fun submit() {
        screenModelScope { state ->
            if (!state.isSubmitEnabled) return@screenModelScope
            val zone = coachZone ?: return@screenModelScope
            val time = parseTime(state.timeText) ?: return@screenModelScope

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

    private fun defaultWeekDays(date: LocalDate): List<WeekDayToggle> {
        return DayOfWeek.entries.map { dayOfWeek ->
            WeekDayToggle(
                dayOfWeek = dayOfWeek,
                label = WEEKDAY_LABELS[dayOfWeek.ordinal],
                isSelected = dayOfWeek == date.dayOfWeek,
            )
        }
    }

    private fun normalizeTime(raw: String): String {
        val digits = raw.filter(Char::isDigit).take(4)
        return when {
            digits.length <= 2 -> digits
            else -> "${digits.take(2)}$TIME_SEPARATOR${digits.drop(2)}"
        }
    }

    private fun parseTime(text: String): LocalTime? {
        val parts = text.split(TIME_SEPARATOR)
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        if (hours !in 0..23 || minutes !in 0 until MINUTES_IN_HOUR) return null
        return LocalTime(hour = hours, minute = minutes)
    }

    private fun formatDate(date: LocalDate): String {
        val month = MONTH_NAMES[date.monthNumber - 1]
        return "${date.dayOfMonth} $month"
    }
}
