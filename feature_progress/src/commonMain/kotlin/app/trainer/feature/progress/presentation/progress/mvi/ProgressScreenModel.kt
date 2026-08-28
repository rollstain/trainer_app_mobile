package app.trainer.feature.progress.presentation.progress.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.monthGenitiveOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.base.input.WeightInput
import app.trainer.base.metrics.MetricChart
import app.trainer.base.metrics.MetricSample
import app.trainer.base.metrics.ProgressMetric
import app.trainer.base.metrics.metricChartOf
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
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.getString

private const val CHECK_IN_HISTORY_DAYS = 30
private const val PHOTO_STRIP_LIMIT = 6
private const val HABIT_WEEK_DAYS = 7
private const val MILLIMETERS_IN_CENTIMETER = 10
private const val SUMMARY_SEPARATOR = " · "

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
        onFetchDataScope { load(showsShimmer = true) }
    }

    private suspend fun load(showsShimmer: Boolean) {
        updateState { it.copy(isLoading = showsShimmer, failure = null) }
        val checkIns = checkInRepository.ownCheckIns(
            from = today.minus(DatePeriod(days = CHECK_IN_HISTORY_DAYS)),
            to = today,
        )
        val habits = habitsRepository.ownHabits(from = weekStart(), to = weekStart().plus(weekLength()))
        if (checkIns is RequestResult.Error && habits is RequestResult.Error) {
            showFailure(checkIns)
            return
        }
        val failedBlocks = buildSet {
            if (checkIns is RequestResult.Error) add(ProgressBlock.CheckIn)
            if (habits is RequestResult.Error) add(ProgressBlock.Habits)
        }
        showLoaded(
            checkIns = checkIns.itemsOrEmpty(),
            habits = habits.itemsOrEmpty(),
            failedBlocks = failedBlocks,
        )
    }

    override fun dispatch(event: ProgressEvent) {
        when (event) {
            ProgressEvent.OnReloadRequested -> onFetchData()
            is ProgressEvent.OnBlockRetryClicked -> screenModelScope { load(showsShimmer = false) }
            ProgressEvent.OnCheckInClicked -> openCheckIn()
            ProgressEvent.OnComparePhotosClicked -> screenModelScope {
                postSideEffect(ProgressSideEffect.OpenPhotoCompare)
            }
            ProgressEvent.OnFormChecksClicked -> screenModelScope {
                postSideEffect(ProgressSideEffect.OpenFormChecks)
            }
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

    private suspend fun showLoaded(
        checkIns: List<CheckIn>,
        habits: List<Habit>,
        failedBlocks: Set<ProgressBlock>,
    ) {
        val latestCheckIn = checkIns.firstOrNull()
        val charts = chartsOf(checkIns)
        val checkInDateLabel = latestCheckIn?.let { formatDate(it.checkInDate) }.orEmpty()
        val checkInSummary = latestCheckIn?.let { formatSummary(it) }.orEmpty()
        val habitRows = habits.map { toRow(it) }
        val photos = checkIns
            .sortedByDescending { it.checkInDate }
            .flatMap { checkIn -> checkIn.photos }
            .take(PHOTO_STRIP_LIMIT)
            .map { photo -> ProgressPhotoRow(photoId = photo.id, url = photo.downloadUrl) }
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
                photos = photos.toImmutableList(),
                failedBlocks = failedBlocks.toImmutableSet(),
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun chartsOf(checkIns: List<CheckIn>): List<MetricChart> = listOfNotNull(
        metricChartOf(
            metric = ProgressMetric.Weight,
            title = getString(Res.string.progress_weight_title),
            samples = samplesOf(checkIns, CheckIn::weightGrams),
            label = ::formatWeight,
        ),
        metricChartOf(
            metric = ProgressMetric.Waist,
            title = getString(Res.string.progress_waist_title),
            samples = samplesOf(checkIns, CheckIn::waistMillimeters),
            label = ::formatLength,
        ),
        metricChartOf(
            metric = ProgressMetric.Chest,
            title = getString(Res.string.progress_chest_title),
            samples = samplesOf(checkIns, CheckIn::chestMillimeters),
            label = ::formatLength,
        ),
        metricChartOf(
            metric = ProgressMetric.Hips,
            title = getString(Res.string.progress_hips_title),
            samples = samplesOf(checkIns, CheckIn::hipsMillimeters),
            label = ::formatLength,
        ),
        metricChartOf(
            metric = ProgressMetric.Wellbeing,
            title = getString(Res.string.progress_wellbeing_title),
            samples = samplesOf(checkIns, CheckIn::wellbeing),
            label = Int::toString,
        ),
        metricChartOf(
            metric = ProgressMetric.Sleep,
            title = getString(Res.string.progress_sleep_title),
            samples = samplesOf(checkIns, CheckIn::sleepQuality),
            label = Int::toString,
        ),
    )

    private fun samplesOf(checkIns: List<CheckIn>, valueOf: (CheckIn) -> Int?): List<MetricSample> =
        checkIns.mapNotNull { checkIn ->
            valueOf(checkIn)?.let { value -> MetricSample(date = checkIn.checkInDate, value = value) }
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

private fun <T> RequestResult<List<T>>.itemsOrEmpty(): List<T> = when (this) {
    is RequestResult.Success -> data
    is RequestResult.Error -> emptyList()
}
