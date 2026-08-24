package app.trainer.feature.progress.presentation.progress.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.input.WeightInput
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.Habit
import app.trainer.data.progress.HabitsRepository
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

private const val CHECK_IN_HISTORY_DAYS = 30
private const val HABIT_WEEK_DAYS = 7
private const val MILLIMETERS_IN_CENTIMETER = 10
private const val SUMMARY_SEPARATOR = " · "

private val MONTH_NAMES = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

private val WEEKDAY_LABELS = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

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
            updateState { it.copy(isLoading = true, isFailed = false) }
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
                latestCheckIn = (checkIns as RequestResult.Success).data.firstOrNull(),
                habits = (habits as RequestResult.Success).data,
            )
        }
    }

    override fun dispatch(event: ProgressEvent) {
        when (event) {
            ProgressEvent.OnRetryClicked -> onFetchData()
            ProgressEvent.OnCheckInClicked -> openCheckIn()
            ProgressEvent.OnHabitAdded -> addHabit()
            is ProgressEvent.OnNewHabitTitleChanged -> updateState { it.copy(newHabitTitle = event.title) }
            is ProgressEvent.OnHabitDayToggled -> toggleDay(habitId = event.habitId, dateIso = event.dateIso)
            is ProgressEvent.OnHabitArchived -> archiveHabit(event.habitId)
        }
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        updateState { it.copy(isLoading = false, isFailed = true) }
        postSideEffect(ProgressSideEffect.ShowFailure(failure))
    }

    private suspend fun showLoaded(latestCheckIn: CheckIn?, habits: List<Habit>) {
        updateState { current ->
            current.copy(
                checkInDateLabel = latestCheckIn?.let { formatDate(it.checkInDate) }.orEmpty(),
                checkInSummary = latestCheckIn?.let(::formatSummary).orEmpty(),
                hasCheckIn = latestCheckIn != null,
                habits = habits.map(::toRow).toImmutableList(),
                isLoading = false,
                isFailed = false,
            )
        }
    }

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

    private fun showDay(habitId: String, dateIso: String, isDone: Boolean) {
        updateState { current ->
            current.copy(
                habits = current.habits
                    .map { row -> if (row.habitId == habitId) withDay(row, dateIso, isDone) else row }
                    .toImmutableList(),
            )
        }
    }

    private fun withDay(row: HabitRow, dateIso: String, isDone: Boolean): HabitRow {
        val days = row.days.map { day -> if (day.dateIso == dateIso) day.copy(isDone = isDone) else day }
        return row.copy(
            days = days.toImmutableList(),
            doneCountLabel = doneCountLabel(days.count { it.isDone }),
        )
    }

    private fun doneCountLabel(doneCount: Int): String = "$doneCount из $HABIT_WEEK_DAYS"

    private fun toRow(habit: Habit): HabitRow {
        val days = weekDates().map { date ->
            HabitDay(
                dateIso = date.toString(),
                weekdayLabel = WEEKDAY_LABELS[date.dayOfWeek.ordinal],
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

    private fun formatSummary(checkIn: CheckIn): String {
        val parts = listOfNotNull(
            checkIn.weightGrams?.let { "${weightInput.toKilogramsText(it)} кг" },
            checkIn.waistMillimeters?.let { "талия ${it / MILLIMETERS_IN_CENTIMETER} см" },
            checkIn.chestMillimeters?.let { "грудь ${it / MILLIMETERS_IN_CENTIMETER} см" },
            checkIn.hipsMillimeters?.let { "бёдра ${it / MILLIMETERS_IN_CENTIMETER} см" },
        )
        return parts.joinToString(separator = SUMMARY_SEPARATOR)
    }

    private fun weekStart(): LocalDate = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))

    private fun weekLength(): DatePeriod = DatePeriod(days = HABIT_WEEK_DAYS - 1)

    private fun weekDates(): List<LocalDate> {
        val start = weekStart()
        return (0 until HABIT_WEEK_DAYS).map { offset -> start.plus(DatePeriod(days = offset)) }
    }

    private fun formatDate(date: LocalDate): String {
        val month = MONTH_NAMES[date.monthNumber - 1]
        return "${date.dayOfMonth} $month"
    }
}
