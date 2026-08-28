package app.trainer.feature.home.presentation.next.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.dayMonthOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.base.input.WeightInput
import app.trainer.data.clients.CoachSummary
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.program.PlannedWorkout
import app.trainer.data.program.ProgramExerciseLine
import app.trainer.data.program.ProgramRepository
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.Habit
import app.trainer.data.progress.HabitsRepository
import app.trainer.data.schedule.ClientScheduleRepository
import app.trainer.data.schedule.ClientSlot
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import app.trainer.feature.home.presentation.startsInLabelOf
import app.trainer.strings.Res
import app.trainer.strings.home_date
import app.trainer.strings.next_fill_check_in
import app.trainer.strings.next_fill_diary
import app.trainer.strings.next_free_slots_summary
import app.trainer.strings.next_habit_done_count
import app.trainer.strings.next_planned_sets
import app.trainer.strings.next_planned_sets_only
import app.trainer.strings.progress_length_value
import app.trainer.strings.progress_no_change_label
import app.trainer.strings.progress_weight_value
import app.trainer.uikit.widgets.HabitWeekDay
import kotlin.time.Clock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private const val SCHEDULE_LOOKAHEAD_WEEKS = 4
private const val CHECK_IN_HISTORY_DAYS = 90
private const val DIARY_HISTORY_DAYS = 30
private const val HABIT_WEEK_DAYS = 7
private const val CHECK_IN_OVERDUE_DAYS = 14
private const val CHART_MIN_POINTS = 2
private const val MILLIMETERS_IN_CENTIMETER = 10
private const val SUMMARY_SEPARATOR = " · "
private const val RANGE_SEPARATOR = " — "
private const val INCREASE_SIGN = "+"
private const val DECREASE_SIGN = "−"

class NextScreenModel(
    private val participantsRepository: ParticipantsRepository,
    private val profileRepository: ProfileRepository,
    private val scheduleRepository: ClientScheduleRepository,
    private val trainingLogRepository: TrainingLogRepository,
    private val checkInRepository: CheckInRepository,
    private val habitsRepository: HabitsRepository,
    private val programRepository: ProgramRepository,
    private val weightInput: WeightInput,
    private val weeks: ScheduleWeeks,
) : BaseScreenModel<NextState, NextSideEffect, NextEvent>(
    initialState = NextState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope { load() }
    }

    override fun dispatch(event: NextEvent) {
        when (event) {
            NextEvent.OnRetryClicked -> onFetchData()
            NextEvent.OnProfileClicked -> post(NextSideEffect.OpenProfile)
            NextEvent.OnBookingClicked -> post(NextSideEffect.OpenBooking)
            NextEvent.OnChatClicked -> post(NextSideEffect.OpenChat)
            NextEvent.OnInviteCodeClicked -> post(NextSideEffect.OpenInvite)
            is NextEvent.OnFillClicked -> openFill(event.kind)
        }
    }

    private fun post(effect: NextSideEffect) {
        screenModelScope { postSideEffect(effect) }
    }

    private fun openFill(kind: FillKind) {
        screenModelScope {
            val dateIso = todayIn(TimeZone.currentSystemDefault()).toString()
            when (kind) {
                FillKind.Diary -> postSideEffect(NextSideEffect.OpenDiary(dateIso = dateIso))
                FillKind.CheckIn -> postSideEffect(NextSideEffect.OpenCheckIn(dateIso = dateIso))
            }
        }
    }

    private fun todayIn(zone: TimeZone): LocalDate = Clock.System.now().toLocalDateTime(zone).date

    private suspend fun load() {
        updateState { it.copy(isLoading = true, failure = null) }

        val profile = profileRepository.me()
        if (profile is RequestResult.Error) return showFailure(profile)
        val clientDisplayName = (profile as RequestResult.Success).data.displayName
        updateState { it.copy(clientDisplayName = clientDisplayName) }

        val coaches = participantsRepository.coachesOfClient()
        if (coaches is RequestResult.Error) return showFailure(coaches)
        val coach = (coaches as RequestResult.Success).data.firstOrNull()
        if (coach == null) {
            updateState { it.copy(session = NextSessionCard.NoCoach, isLoading = false, failure = null) }
            return
        }

        val zone = weeks.parseZone(coach.zoneId) ?: TimeZone.currentSystemDefault()
        val today = todayIn(zone)
        val weekStart = weeks.weekStartOf(today)

        val schedule = scheduleRepository.clientSchedule(
            coachId = coach.coachId,
            from = weeks.startInstant(weekStart = weekStart, zone = zone),
            to = weeks.endInstant(
                weekStart = weeks.shiftWeeks(weekStart = weekStart, weeks = SCHEDULE_LOOKAHEAD_WEEKS - 1),
                zone = zone,
            ),
        )
        if (schedule is RequestResult.Error) return showFailure(schedule)

        val entries = trainingLogRepository.ownEntries(
            from = today.minus(DatePeriod(days = DIARY_HISTORY_DAYS - 1)),
            to = today,
        )
        if (entries is RequestResult.Error) return showFailure(entries)

        val checkIns = checkInRepository.ownCheckIns(
            from = today.minus(DatePeriod(days = CHECK_IN_HISTORY_DAYS - 1)),
            to = today,
        )
        if (checkIns is RequestResult.Error) return showFailure(checkIns)

        val planned = programRepository.plannedWorkouts(from = today, to = today)
        if (planned is RequestResult.Error) return showFailure(planned)

        val weekEnd = today.plus(DatePeriod(days = HABIT_WEEK_DAYS - 1))
        val habits = habitsRepository.ownHabits(from = weeks.weekStartOf(today), to = weekEnd)
        if (habits is RequestResult.Error) return showFailure(habits)

        show(
            coach = coach,
            zone = zone,
            today = today,
            slots = (schedule as RequestResult.Success).data.slots,
            lastEntryDate = (entries as RequestResult.Success).data.maxOfOrNull { it.entryDate },
            checkIns = (checkIns as RequestResult.Success).data.sortedBy { it.checkInDate },
            habits = (habits as RequestResult.Success).data,
            planned = (planned as RequestResult.Success).data.firstOrNull(),
        )
    }

    private suspend fun show(
        coach: CoachSummary,
        zone: TimeZone,
        today: LocalDate,
        slots: List<ClientSlot>,
        lastEntryDate: LocalDate?,
        checkIns: List<CheckIn>,
        habits: List<Habit>,
        planned: PlannedWorkout?,
    ) {
        val now = Clock.System.now()
        val upcoming = slots.filter { it.startsAt >= now }.sortedBy { it.startsAt }
        val booked = upcoming.firstOrNull { it.isBookedByMe }
        val freeSlots = upcoming.filter { it.isAvailable && !it.isBookedByMe }

        val session = when {
            booked != null -> {
                val date = weeks.dateOf(booked.startsAt, zone)
                NextSessionCard.Booked(
                    dayLabel = getString(
                        Res.string.home_date,
                        weekdayShortOf(date).lowercase(),
                        dayMonthOf(date),
                    ),
                    timeLabel = timeOfDayOf(booked.startsAt.toLocalDateTime(zone)),
                    startsInLabel = startsInLabelOf(startsAt = booked.startsAt, now = now),
                    coachDisplayName = coach.displayName,
                    isToday = date == today,
                    canRequestChange = booked.canRequestChange,
                )
            }
            freeSlots.isNotEmpty() -> NextSessionCard.SlotsAvailable(
                summary = getString(Res.string.next_free_slots_summary, freeSlots.size)
            )
            else -> NextSessionCard.NoSlots
        }

        val weekStart = weeks.weekStartOf(today)
        val weekDates = (0 until HABIT_WEEK_DAYS).map { weekStart.plus(DatePeriod(days = it)) }

        val fills = fillRowsOf(
            today = today,
            lastEntryDate = lastEntryDate,
            lastCheckInDate = checkIns.lastOrNull()?.checkInDate,
        ).toImmutableList()
        val habitRows = habits
            .map { habit -> habitRowOf(habit = habit, weekDates = weekDates, today = today) }
            .toImmutableList()
        val weekdayLabels = weekDates.map { weekdayShortOf(it) }.toImmutableList()
        val dynamics = dynamicsOf(checkIns = checkIns)
        val plannedToday = plannedTodayOf(planned)

        updateState { current ->
            current.copy(
                session = session,
                planned = plannedToday,
                fills = fills,
                habits = habitRows,
                weekdayLabels = weekdayLabels,
                dynamics = dynamics,
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun plannedTodayOf(planned: PlannedWorkout?): PlannedToday {
        if (planned == null || planned.exercises.isEmpty()) return PlannedToday.None
        return PlannedToday.Workout(
            programTitle = planned.programTitle,
            dayTitle = planned.dayTitle,
            exercises = planned.exercises
                .mapIndexed { index, line -> plannedRowOf(index = index, line = line) }
                .toImmutableList(),
        )
    }

    private suspend fun plannedRowOf(index: Int, line: ProgramExerciseLine): PlannedExerciseRow {
        val volume = when (val repetitions = line.repetitions) {
            null -> getString(Res.string.next_planned_sets_only, line.setsCount)
            else -> getString(Res.string.next_planned_sets, line.setsCount, repetitions)
        }
        val weight = line.weightGrams?.let { grams -> formatWeight(grams) }
        return PlannedExerciseRow(
            exerciseId = "${line.exerciseId}-$index",
            name = line.exerciseName,
            details = listOfNotNull(volume, weight).joinToString(separator = SUMMARY_SEPARATOR),
        )
    }

    private suspend fun fillRowsOf(
        today: LocalDate,
        lastEntryDate: LocalDate?,
        lastCheckInDate: LocalDate?,
    ): List<FillRow> = listOf(
        FillRow(
            kind = FillKind.Diary,
            title = getString(Res.string.next_fill_diary),
            status = diaryStatusOf(today = today, lastEntryDate = lastEntryDate),
        ),
        FillRow(
            kind = FillKind.CheckIn,
            title = getString(Res.string.next_fill_check_in),
            status = checkInStatusOf(today = today, lastCheckInDate = lastCheckInDate),
        ),
    )

    private fun diaryStatusOf(today: LocalDate, lastEntryDate: LocalDate?): FillStatus =
        if (lastEntryDate == today) FillStatus.DoneToday else FillStatus.Pending

    private fun checkInStatusOf(today: LocalDate, lastCheckInDate: LocalDate?): FillStatus {
        if (lastCheckInDate == null) return FillStatus.NeverFilled
        if (lastCheckInDate == today) return FillStatus.DoneToday
        val days = lastCheckInDate.daysUntil(today)
        return if (days >= CHECK_IN_OVERDUE_DAYS) FillStatus.Overdue(days = days) else FillStatus.Pending
    }

    private suspend fun habitRowOf(
        habit: Habit,
        weekDates: List<LocalDate>,
        today: LocalDate,
    ): NextHabitRow {
        val doneDates = habit.doneDates.toSet()
        return NextHabitRow(
            habitId = habit.id,
            title = habit.title,
            isSetByCoach = habit.isSetByCoach,
            doneCountLabel = getString(
                Res.string.next_habit_done_count,
                weekDates.count { it in doneDates },
                HABIT_WEEK_DAYS,
            ),
            days = weekDates
                .map { date ->
                    when {
                        date in doneDates -> HabitWeekDay.Done
                        date > today -> HabitWeekDay.Future
                        else -> HabitWeekDay.Missed
                    }
                }
                .toImmutableList(),
        )
    }

    private suspend fun dynamicsOf(checkIns: List<CheckIn>): NextDynamics {
        val samples = checkIns.mapNotNull { checkIn ->
            checkIn.weightGrams?.let { grams -> checkIn.checkInDate to grams }
        }
        if (samples.size < CHART_MIN_POINTS) return NextDynamics.NoCheckIns
        val values = samples.map { it.second }
        val delta = values.last() - values.first()
        val latest = checkIns.last()
        return NextDynamics.Weight(
            valueLabel = formatWeight(values.last()),
            dateLabel = dayMonthOf(samples.last().first),
            deltaLabel = when {
                delta > 0 -> INCREASE_SIGN + formatWeight(delta)
                delta < 0 -> DECREASE_SIGN + formatWeight(-delta)
                else -> getString(Res.string.progress_no_change_label)
            },
            isWeightDown = delta < 0,
            values = values.map(Int::toFloat).toImmutableList(),
            maxLabel = formatWeight(values.max()),
            minLabel = formatWeight(values.min()),
            rangeLabel = dayMonthOf(samples.first().first) + RANGE_SEPARATOR + dayMonthOf(samples.last().first),
            measuresLabel = measuresOf(latest),
        )
    }

    private suspend fun measuresOf(checkIn: CheckIn): String = listOfNotNull(
        checkIn.waistMillimeters?.let { formatLength(it) },
        checkIn.chestMillimeters?.let { formatLength(it) },
        checkIn.hipsMillimeters?.let { formatLength(it) },
        checkIn.wellbeing?.toString(),
        checkIn.sleepQuality?.toString(),
    ).joinToString(separator = SUMMARY_SEPARATOR)

    private suspend fun formatWeight(grams: Int): String =
        getString(Res.string.progress_weight_value, weightInput.toKilogramsText(grams))

    private suspend fun formatLength(millimeters: Int): String =
        getString(Res.string.progress_length_value, millimeters / MILLIMETERS_IN_CENTIMETER)

    private suspend fun showFailure(failure: RequestResult.Error) {
        updateState { it.copy(isLoading = false, failure = failure) }
        postSideEffect(NextSideEffect.ShowFailure(failure))
    }
}
