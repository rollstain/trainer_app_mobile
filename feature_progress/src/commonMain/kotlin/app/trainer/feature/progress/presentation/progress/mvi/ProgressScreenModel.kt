package app.trainer.feature.progress.presentation.progress.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.monthGenitiveOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.base.input.WeightInput
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.Habit
import app.trainer.data.progress.HabitsRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.progress_chest_title
import app.trainer.strings.progress_done_count
import app.trainer.strings.progress_hips_title
import app.trainer.strings.progress_length_value
import app.trainer.strings.progress_no_change_label
import app.trainer.strings.progress_sleep_title
import app.trainer.strings.progress_summary_chest
import app.trainer.strings.progress_summary_hips
import app.trainer.strings.progress_summary_waist
import app.trainer.strings.progress_waist_title
import app.trainer.strings.progress_weight_title
import app.trainer.strings.progress_weight_value
import app.trainer.strings.progress_wellbeing_title
import kotlin.time.Clock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.getString

private const val CHECK_IN_HISTORY_DAYS = 30
private const val HABIT_WEEK_DAYS = 7
private const val MILLIMETERS_IN_CENTIMETER = 10
private const val SUMMARY_SEPARATOR = " · "
private const val CHART_MIN_POINTS = 2
private const val RANGE_SEPARATOR = " — "
private const val INCREASE_SIGN = "+"
private const val DECREASE_SIGN = "−"

class ProgressScreenModel(
    private val checkInRepository: CheckInRepository,
    private val habitsRepository: HabitsRepository,
    private val weightInput: WeightInput,
) : BaseScreenModel<ProgressState, ProgressSideEffect, ProgressEvent>(
    initialState = ProgressState.initial(),
) {

    private val today: LocalDate get() = Clock.System.todayIn(TimeZone.currentSystemDefault())

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            val checkIns = checkInRepository.ownCheckIns(
                from = today.minus(DatePeriod(days = CHECK_IN_HISTORY_DAYS)),
                to = today,
            )
            if (checkIns is RequestResult.Error) {
                showFailure(checkIns)
                return@onFetchDataScope
            }
            val habits = habitsRepository.ownHabits(from = weekStart(), to = weekStart().plus(weekLength()))
            if (habits is RequestResult.Error) {
                showFailure(habits)
                return@onFetchDataScope
            }
            showLoaded(
                checkIns = (checkIns as RequestResult.Success).data,
                habits = (habits as RequestResult.Success).data,
            )
        }
    }

    override fun dispatch(event: ProgressEvent) {
        when (event) {
            ProgressEvent.OnReloadRequested -> onFetchData()
            ProgressEvent.OnCheckInClicked -> openCheckIn()
            is ProgressEvent.OnMetricSelected -> updateState { it.copy(selectedMetric = event.metric) }
            ProgressEvent.OnHabitAdded -> addHabit()
            is ProgressEvent.OnNewHabitTitleChanged -> updateState { it.copy(newHabitTitle = event.title) }
            is ProgressEvent.OnHabitDayToggled -> toggleDay(habitId = event.habitId, dateIso = event.dateIso)
            is ProgressEvent.OnHabitArchived -> archiveHabit(event.habitId)
        }
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        updateState { it.copy(isLoading = false, failure = failure) }
        postSideEffect(ProgressSideEffect.ShowFailure(failure))
    }

    private suspend fun showLoaded(checkIns: List<CheckIn>, habits: List<Habit>) {
        val latestCheckIn = checkIns.firstOrNull()
        val charts = chartsOf(checkIns.sortedBy { it.checkInDate })
        val checkInDateLabel = latestCheckIn?.let { formatDate(it.checkInDate) }.orEmpty()
        val checkInSummary = latestCheckIn?.let { formatSummary(it) }.orEmpty()
        val habitRows = habits.map { toRow(it) }
        updateState { current ->
            current.copy(
                checkInDateLabel = checkInDateLabel,
                checkInSummary = checkInSummary,
                hasCheckIn = latestCheckIn != null,
                coachReply = latestCheckIn?.coachComment
                    ?.let(CoachReply::Text)
                    ?: CoachReply.None,
                charts = charts.toImmutableList(),
                selectedMetric = charts
                    .map { it.metric }
                    .firstOrNull { it == current.selectedMetric }
                    ?: charts.firstOrNull()?.metric,
                habits = habitRows.toImmutableList(),
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun chartsOf(checkIns: List<CheckIn>): List<MetricChart> = listOfNotNull(
        chartOf(
            metric = ProgressMetric.Weight,
            title = getString(Res.string.progress_weight_title),
            checkIns = checkIns,
            valueOf = CheckIn::weightGrams,
            label = ::formatWeight,
        ),
        chartOf(
            metric = ProgressMetric.Waist,
            title = getString(Res.string.progress_waist_title),
            checkIns = checkIns,
            valueOf = CheckIn::waistMillimeters,
            label = ::formatLength,
        ),
        chartOf(
            metric = ProgressMetric.Chest,
            title = getString(Res.string.progress_chest_title),
            checkIns = checkIns,
            valueOf = CheckIn::chestMillimeters,
            label = ::formatLength,
        ),
        chartOf(
            metric = ProgressMetric.Hips,
            title = getString(Res.string.progress_hips_title),
            checkIns = checkIns,
            valueOf = CheckIn::hipsMillimeters,
            label = ::formatLength,
        ),
        chartOf(
            metric = ProgressMetric.Wellbeing,
            title = getString(Res.string.progress_wellbeing_title),
            checkIns = checkIns,
            valueOf = CheckIn::wellbeing,
            label = Int::toString,
        ),
        chartOf(
            metric = ProgressMetric.Sleep,
            title = getString(Res.string.progress_sleep_title),
            checkIns = checkIns,
            valueOf = CheckIn::sleepQuality,
            label = Int::toString,
        ),
    )

    private suspend fun chartOf(
        metric: ProgressMetric,
        title: String,
        checkIns: List<CheckIn>,
        valueOf: (CheckIn) -> Int?,
        label: suspend (Int) -> String,
    ): MetricChart? {
        val samples = checkIns.mapNotNull { checkIn ->
            valueOf(checkIn)?.let { value -> checkIn.checkInDate to value }
        }
        if (samples.size < CHART_MIN_POINTS) return null
        val values = samples.map { it.second }
        val delta = values.last() - values.first()
        return MetricChart(
            metric = metric,
            title = title,
            values = values.map(Int::toFloat).toImmutableList(),
            maxLabel = label(values.max()),
            minLabel = label(values.min()),
            rangeLabel = formatDate(samples.first().first) +
                RANGE_SEPARATOR +
                formatDate(samples.last().first),
            latestLabel = label(values.last()),
            deltaLabel = when {
                delta > 0 -> INCREASE_SIGN + label(delta)
                delta < 0 -> DECREASE_SIGN + label(-delta)
                else -> getString(Res.string.progress_no_change_label)
            },
        )
    }

    private suspend fun formatWeight(grams: Int): String =
        getString(Res.string.progress_weight_value, weightInput.toKilogramsText(grams))

    private suspend fun formatLength(millimeters: Int): String =
        getString(Res.string.progress_length_value, millimeters / MILLIMETERS_IN_CENTIMETER)

    private fun openCheckIn() {
        screenModelScope {
            postSideEffect(ProgressSideEffect.OpenCheckIn(dateIso = today.toString()))
        }
    }

    private fun addHabit() {
        screenModelScope { state ->
            val title = state.newHabitTitle.trim()
            if (title.isEmpty()) return@screenModelScope
            when (val created = habitsRepository.createOwn(title = title)) {
                is RequestResult.Error -> postSideEffect(ProgressSideEffect.ShowFailure(created))
                is RequestResult.Success -> {
                    updateState { it.copy(newHabitTitle = "") }
                    onFetchData()
                }
            }
        }
    }

    private fun archiveHabit(habitId: String) {
        screenModelScope {
            when (val archived = habitsRepository.archive(habitId = habitId)) {
                is RequestResult.Error -> postSideEffect(ProgressSideEffect.ShowFailure(archived))
                is RequestResult.Success -> onFetchData()
            }
        }
    }

    private fun toggleDay(habitId: String, dateIso: String) {
        screenModelScope { state ->
            val habit = state.habits.firstOrNull { it.habitId == habitId } ?: return@screenModelScope
            val day = habit.days.firstOrNull { it.dateIso == dateIso } ?: return@screenModelScope
            if (day.isFuture) return@screenModelScope
            val isDone = !day.isDone
            showDay(habitId = habitId, dateIso = dateIso, isDone = isDone)

            val marked = habitsRepository.mark(
                habitId = habitId,
                date = LocalDate.parse(dateIso),
                isDone = isDone,
            )
            if (marked is RequestResult.Error) {
                showDay(habitId = habitId, dateIso = dateIso, isDone = day.isDone)
                postSideEffect(ProgressSideEffect.ShowFailure(marked))
            }
        }
    }

    private suspend fun showDay(habitId: String, dateIso: String, isDone: Boolean) {
        val row = state.habits.firstOrNull { it.habitId == habitId } ?: return
        val updated = withDay(row = row, dateIso = dateIso, isDone = isDone)
        updateState { current ->
            current.copy(
                habits = current.habits
                    .map { existing -> if (existing.habitId == habitId) updated else existing }
                    .toImmutableList(),
            )
        }
    }

    private suspend fun withDay(row: HabitRow, dateIso: String, isDone: Boolean): HabitRow {
        val days = row.days.map { day -> if (day.dateIso == dateIso) day.copy(isDone = isDone) else day }
        return row.copy(
            days = days.toImmutableList(),
            doneCountLabel = doneCountLabel(days.count { it.isDone }),
        )
    }

    private suspend fun doneCountLabel(doneCount: Int): String =
        getString(Res.string.progress_done_count, doneCount, HABIT_WEEK_DAYS)

    private suspend fun toRow(habit: Habit): HabitRow {
        val days = weekDates().map { date ->
            HabitDay(
                dateIso = date.toString(),
                weekdayLabel = weekdayShortOf(date),
                isDone = habit.doneDates.contains(date),
                isToday = date == today,
                isFuture = date > today,
            )
        }
        return HabitRow(
            habitId = habit.id,
            title = habit.title,
            isSetByCoach = habit.isSetByCoach,
            doneCountLabel = doneCountLabel(days.count { it.isDone }),
            days = days.toImmutableList(),
        )
    }

    private suspend fun formatSummary(checkIn: CheckIn): String {
        val parts = listOfNotNull(
            checkIn.weightGrams?.let { formatWeight(it) },
            checkIn.waistMillimeters?.let { getString(Res.string.progress_summary_waist, formatLength(it)) },
            checkIn.chestMillimeters?.let { getString(Res.string.progress_summary_chest, formatLength(it)) },
            checkIn.hipsMillimeters?.let { getString(Res.string.progress_summary_hips, formatLength(it)) },
        )
        return parts.joinToString(separator = SUMMARY_SEPARATOR)
    }

    private fun weekStart(): LocalDate = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))

    private fun weekLength(): DatePeriod = DatePeriod(days = HABIT_WEEK_DAYS - 1)

    private fun weekDates(): List<LocalDate> {
        val start = weekStart()
        return (0 until HABIT_WEEK_DAYS).map { offset -> start.plus(DatePeriod(days = offset)) }
    }

    private suspend fun formatDate(date: LocalDate): String {
        val month = monthGenitiveOf(date)
        return "${date.day} $month"
    }
}
