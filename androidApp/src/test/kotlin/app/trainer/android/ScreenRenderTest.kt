package app.trainer.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontFamily
import app.trainer.base.metrics.MetricChart
import app.trainer.base.metrics.ProgressMetric
import app.trainer.data.clients.CoachPolicy
import app.trainer.data.schedule.SlotStatus
import app.trainer.feature.account.profile.mvi.ProfileState
import app.trainer.feature.account.profile.ui.ProfileView
import app.trainer.feature.clientcard.presentation.mvi.CheckInReview
import app.trainer.feature.clientcard.presentation.mvi.CheckInRow
import app.trainer.feature.clientcard.presentation.mvi.ClientCardState
import app.trainer.feature.clientcard.presentation.ui.ClientCardView
import app.trainer.feature.home.presentation.next.mvi.FillKind
import app.trainer.feature.home.presentation.next.mvi.FillRow
import app.trainer.feature.home.presentation.next.mvi.FillStatus
import app.trainer.feature.home.presentation.next.mvi.NextDynamics
import app.trainer.feature.home.presentation.next.mvi.NextHabitRow
import app.trainer.feature.home.presentation.next.mvi.NextSessionCard
import app.trainer.feature.home.presentation.next.mvi.NextState
import app.trainer.feature.home.presentation.next.mvi.PlannedExerciseRow
import app.trainer.feature.home.presentation.next.mvi.PlannedToday
import app.trainer.feature.home.presentation.next.ui.NextView
import app.trainer.feature.home.presentation.today.mvi.LapsedSince
import app.trainer.feature.home.presentation.today.mvi.TodayCheckInRow
import app.trainer.feature.home.presentation.today.mvi.TodayDialogRow
import app.trainer.feature.home.presentation.today.mvi.TodayFreeSlots
import app.trainer.feature.home.presentation.today.mvi.TodayLapsedRow
import app.trainer.feature.home.presentation.today.mvi.TodayNextSession
import app.trainer.feature.home.presentation.today.mvi.TodaySessionRow
import app.trainer.feature.home.presentation.today.mvi.TodayState
import app.trainer.feature.home.presentation.today.mvi.TodayTomorrow
import app.trainer.feature.home.presentation.today.ui.TodayView
import app.trainer.feature.progress.presentation.formcheck.mvi.AwaitingFormCheck
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachAnswer
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachFormChecksState
import app.trainer.feature.progress.presentation.formcheck.mvi.FormCheckRow
import app.trainer.feature.progress.presentation.formcheck.mvi.FormChecksState
import app.trainer.feature.progress.presentation.formcheck.ui.CoachFormChecksView
import app.trainer.feature.progress.presentation.formcheck.ui.FormChecksView
import app.trainer.feature.progress.presentation.photos.mvi.CompareSide
import app.trainer.feature.progress.presentation.photos.mvi.PhotoCompareState
import app.trainer.feature.progress.presentation.photos.mvi.PhotoShot
import app.trainer.feature.progress.presentation.photos.ui.PhotoCompareView
import app.trainer.feature.progress.presentation.progress.mvi.HabitDay
import app.trainer.feature.progress.presentation.progress.mvi.HabitRow
import app.trainer.feature.progress.presentation.progress.mvi.ProgressPhotoRow
import app.trainer.feature.progress.presentation.progress.mvi.ProgressState
import app.trainer.feature.progress.presentation.progress.ui.ProgressView
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleState
import app.trainer.feature.schedule.presentation.coach.mvi.CoachSlotRow
import app.trainer.feature.schedule.presentation.coach.mvi.ScheduleDay
import app.trainer.feature.schedule.presentation.coach.ui.CoachScheduleView
import app.trainer.feature.traininglog.presentation.editor.mvi.PlannedForDay
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorState
import app.trainer.feature.traininglog.presentation.editor.ui.TrainingLogEditorView
import app.trainer.feature.traininglog.presentation.programday.mvi.ExerciseChoice
import app.trainer.feature.traininglog.presentation.programday.mvi.ExerciseLineRow
import app.trainer.feature.traininglog.presentation.programday.mvi.ProgramDayState
import app.trainer.feature.traininglog.presentation.programday.ui.ProgramDayView
import app.trainer.feature.traininglog.presentation.programeditor.mvi.DayContent
import app.trainer.feature.traininglog.presentation.programeditor.mvi.DayRow
import app.trainer.feature.traininglog.presentation.programeditor.mvi.ProgramEditorState
import app.trainer.feature.traininglog.presentation.programeditor.ui.ProgramEditorView
import app.trainer.feature.traininglog.presentation.programs.mvi.NewProgramDraft
import app.trainer.feature.traininglog.presentation.programs.mvi.ProgramRow
import app.trainer.feature.traininglog.presentation.programs.mvi.ProgramsState
import app.trainer.feature.traininglog.presentation.programs.ui.ProgramsView
import app.trainer.uikit.AppTheme
import app.trainer.uikit.widgets.HabitWeekDay
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val MINUTES_IN_HOUR = 60
private const val SLOT_DURATION_MINUTES = 60
private val MONDAY = LocalDate(2026, 8, 24)
private const val CANCELLATION_WINDOW_HOURS = 12
private const val REMINDER_HOUR = 10
private const val REMINDERS_TITLE = "Client reminders"
private const val COMPARE_BEFORE = "Before"
private const val COMPARE_AFTER = "After"
private const val CLIENT_WEIGHT_LATEST = "82,4 kg"
private const val CLIENT_WEIGHT_DELTA = "−1,6 kg"
private const val COMPARE_EMPTY_TITLE = "Nothing to compare yet"
private const val PROGRESS_PHOTOS_SECTION = "Photos"
private const val PROGRESS_PHOTOS_COMPARE = "Compare"
private const val FORM_CHECK_AWAITING = "Your coach has not replied yet"
private const val FORM_CHECK_APPROVED = "Your coach watched it — nothing to fix"
private const val FORM_CHECK_SEND = "Send a video"
private const val FORM_CHECK_REPLY = "Reply"
private const val FORM_CHECK_CLIENT_NAME = "Anna"
private const val PROGRESS_PHOTOS_EMPTY = "Attach a photo to a check-in and it will show up here"
private const val FIRST_SHOT_DATE = "3 June"
private const val LAST_SHOT_DATE = "28 August"
private const val REMINDER_HOUR_LABEL = "10:00"
private const val DIARY_REMINDER_TITLE = "When the diary is a week idle"

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = android.app.Application::class,
    qualifiers = "w411dp-h891dp-xhdpi",
)
class ScreenRenderTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `coach calendar renders a week full of slots`() {
        compose.setContent {
            TestTheme {
                CoachScheduleView(state = coachScheduleWithSlots(), onEvent = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `coach calendar renders an empty week`() {
        compose.setContent {
            TestTheme {
                CoachScheduleView(
                    state = coachScheduleWithSlots().copy(
                        days = coachScheduleWithSlots().days
                            .map { it.copy(slots = persistentListOf()) }
                            .let { days -> persistentListOf(*days.toTypedArray()) },
                    ),
                    onEvent = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `coach today renders every block`() {
        compose.setContent {
            TestTheme {
                TodayView(state = todayWithBlocks(), onEvent = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `coach today renders the quiet day`() {
        compose.setContent {
            TestTheme {
                TodayView(
                    state = todayWithBlocks().copy(
                        sessions = persistentListOf(),
                        unread = persistentListOf(),
                        moreUnreadCount = 0,
                        lapsed = persistentListOf(),
                        awaitingCheckIns = persistentListOf(),
                    ),
                    onEvent = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `client next renders a booked session`() {
        compose.setContent {
            TestTheme {
                NextView(state = nextWithSession(), onEvent = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `client next renders without a coach`() {
        compose.setContent {
            TestTheme {
                NextView(
                    state = nextWithSession().copy(
                        session = NextSessionCard.NoCoach,
                        habits = persistentListOf(),
                        dynamics = NextDynamics.NoCheckIns,
                    ),
                    onEvent = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the program list renders programs`() {
        compose.setContent {
            TestTheme {
                ProgramsView(state = programsWithOne(), onEvent = {}, onBackClick = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the program list renders the new program sheet`() {
        compose.setContent {
            TestTheme {
                ProgramsView(
                    state = programsWithOne().copy(draft = NewProgramDraft.empty()),
                    onEvent = {},
                    onBackClick = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the program editor renders a week of days`() {
        compose.setContent {
            TestTheme {
                ProgramEditorView(state = programEditorWithDays(), onEvent = {}, onBackClick = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the program day editor renders its exercise lines`() {
        compose.setContent {
            TestTheme {
                ProgramDayView(state = programDayWithLines(), onEvent = {}, onBackClick = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `client next renders the workout planned for today`() {
        compose.setContent {
            TestTheme {
                NextView(
                    state = nextWithSession().copy(
                        planned = PlannedToday.Workout(
                            programTitle = "Набор массы",
                            dayTitle = "День ног",
                            exercises = persistentListOf(
                                PlannedExerciseRow(
                                    exerciseId = "line-1",
                                    name = "Приседания",
                                    details = "4×8 · 60 кг",
                                ),
                                PlannedExerciseRow(
                                    exerciseId = "line-2",
                                    name = "Выпады",
                                    details = "3 подх",
                                ),
                            ),
                        ),
                    ),
                    onEvent = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the diary editor offers to take the planned workout`() {
        compose.setContent {
            TestTheme {
                TrainingLogEditorView(
                    state = TrainingLogEditorState.initial(entryDate = MONDAY).copy(
                        dateLabel = "24 августа",
                        volumeLabel = "1,8 т",
                        planned = PlannedForDay.Workout(
                            dayTitle = "День ног",
                            summary = "4 упр · 14 подходов",
                        ),
                        isLoading = false,
                    ),
                    onEvent = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the coach profile renders the reminder settings`() {
        compose.setContent {
            TestTheme {
                ProfileView(state = coachProfile(), onEvent = {}, onBackClick = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(REMINDERS_TITLE).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(DIARY_REMINDER_TITLE).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(REMINDER_HOUR_LABEL).assertExists()
    }

    @Test
    fun `a client sees whether the coach answered each video`() {
        compose.setContent {
            TestTheme {
                FormChecksView(state = formChecks(), onEvent = {}, onSendClick = {}, onBackClick = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(FORM_CHECK_AWAITING).assertIsDisplayed()
        compose.onNodeWithText(FORM_CHECK_APPROVED).assertIsDisplayed()
        compose.onNodeWithText(FORM_CHECK_SEND).assertIsDisplayed()
    }

    @Test
    fun `the coach queue offers a reply for every waiting video`() {
        compose.setContent {
            TestTheme {
                CoachFormChecksView(state = coachFormChecks(), onEvent = {}, onBackClick = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(FORM_CHECK_CLIENT_NAME).assertIsDisplayed()
        compose.onNodeWithText(FORM_CHECK_REPLY).assertIsDisplayed()
    }

    @Test
    fun `progress offers to compare the photos it shows`() {
        compose.setContent {
            TestTheme {
                ProgressView(
                    state = progressWithCharts().copy(
                        photos = persistentListOf(
                            ProgressPhotoRow(photoId = "photo-0", url = "https://example.invalid/0.jpg"),
                            ProgressPhotoRow(photoId = "photo-1", url = "https://example.invalid/1.jpg"),
                        ),
                    ),
                    onEvent = {},
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(PROGRESS_PHOTOS_SECTION).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(PROGRESS_PHOTOS_COMPARE).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `progress explains where photos come from when there are none`() {
        compose.setContent {
            TestTheme {
                ProgressView(state = progressWithCharts(), onEvent = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(PROGRESS_PHOTOS_EMPTY).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `photo comparison shows the first and the last shot`() {
        compose.setContent {
            TestTheme {
                PhotoCompareView(state = photosToCompare(), onEvent = {}, onBackClick = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(COMPARE_BEFORE).assertIsDisplayed()
        compose.onNodeWithText(COMPARE_AFTER).assertIsDisplayed()
        compose.onNodeWithText(FIRST_SHOT_DATE).assertIsDisplayed()
        compose.onNodeWithText(LAST_SHOT_DATE).assertIsDisplayed()
    }

    @Test
    fun `photo comparison asks for a second shot when there is only one`() {
        compose.setContent {
            TestTheme {
                PhotoCompareView(
                    state = photosToCompare().copy(shots = persistentListOf(shot(index = 0))),
                    onEvent = {},
                    onBackClick = {},
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(COMPARE_EMPTY_TITLE).assertIsDisplayed()
    }

    @Test
    fun `the coach sees how the client's weight moved`() {
        compose.setContent {
            TestTheme {
                ClientCardView(state = clientCardWithDynamics(), onEvent = {}, onBackClick = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(CLIENT_WEIGHT_LATEST).assertIsDisplayed()
        compose.onNodeWithText(CLIENT_WEIGHT_DELTA).assertIsDisplayed()
    }

    @Test
    fun `progress renders charts and habits`() {
        compose.setContent {
            TestTheme {
                ProgressView(state = progressWithCharts(), onEvent = {})
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `progress renders without check-in history`() {
        compose.setContent {
            TestTheme {
                ProgressView(
                    state = progressWithCharts().copy(
                        charts = persistentListOf(),
                        selectedMetric = null,
                        hasCheckIn = false,
                        checkInSummary = "",
                    ),
                    onEvent = {},
                )
            }
        }
        compose.waitForIdle()
    }
}

@androidx.compose.runtime.Composable
private fun TestTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    AppTheme(
        textFontFamily = FontFamily.Default,
        numericFontFamily = FontFamily.Monospace,
        content = content,
    )
}

private fun photosToCompare(): PhotoCompareState = PhotoCompareState.initial().copy(
    shots = persistentListOf(shot(index = 0), shot(index = 1)),
    beforePhotoId = "photo-0",
    afterPhotoId = "photo-1",
    selectedSide = CompareSide.After,
    isLoading = false,
)

private fun shot(index: Int): PhotoShot = PhotoShot(
    photoId = "photo-$index",
    url = "https://example.invalid/photo-$index.jpg",
    dateIso = if (index == 0) "2026-06-03" else "2026-08-28",
    dateLabel = if (index == 0) FIRST_SHOT_DATE else LAST_SHOT_DATE,
)

private fun formChecks(): FormChecksState = FormChecksState.initial().copy(
    checks = persistentListOf(
        FormCheckRow(
            formCheckId = "check-1",
            dateLabel = "28.08",
            note = "Правильно ли держу спину?",
            videoUrl = null,
            answer = CoachAnswer.Awaiting,
        ),
        FormCheckRow(
            formCheckId = "check-2",
            dateLabel = "20.08",
            note = null,
            videoUrl = null,
            answer = CoachAnswer.Approved,
        ),
    ),
    isLoading = false,
)

private fun coachFormChecks(): CoachFormChecksState = CoachFormChecksState.initial().copy(
    checks = persistentListOf(
        AwaitingFormCheck(
            formCheckId = "check-1",
            clientDisplayName = FORM_CHECK_CLIENT_NAME,
            dateLabel = "28.08",
            exerciseName = "Squat",
            note = "Правильно ли держу спину?",
            videoUrl = null,
            draft = "",
            isSending = false,
        ),
    ),
    isLoading = false,
)

private fun coachProfile(): ProfileState = ProfileState.initial().copy(
    displayName = "Иван",
    roleLabel = "Тренер",
    contactLabel = "+7 900 000-00-00",
    policy = CoachPolicy(
        cancellationWindowHours = CANCELLATION_WINDOW_HOURS,
        reminderHour = REMINDER_HOUR,
        sessionRemindersEnabled = true,
        diaryRemindersEnabled = true,
        checkInRemindersEnabled = false,
    ),
    isCoach = true,
    isLoading = false,
)

private fun coachScheduleWithSlots(): CoachScheduleState {
    val days = (0 until 7).map { offset ->
        val date = LocalDate(MONDAY.year, MONDAY.month, MONDAY.day + offset)
        ScheduleDay(
            date = date,
            weekdayLabel = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")[offset],
            dayNumberLabel = date.day.toString(),
            isToday = offset == 0,
            isWeekend = offset >= 5,
            slots = persistentListOf(
                slotAt(hour = 8, index = offset, status = SlotStatus.BOOKED, client = "Мария"),
                slotAt(hour = 12, index = offset, status = SlotStatus.FREE, client = null),
                slotAt(hour = 19, index = offset, status = SlotStatus.COMPLETED, client = "Анна"),
            ),
        )
    }
    return CoachScheduleState.initial().copy(
        weekStart = MONDAY,
        weekTitle = "24—30 августа",
        days = persistentListOf(*days.toTypedArray()),
        isLoading = false,
    )
}

private fun slotAt(hour: Int, index: Int, status: SlotStatus, client: String?): CoachSlotRow =
    CoachSlotRow(
        slotId = "slot-$hour-$index",
        startMinutesOfDay = hour * MINUTES_IN_HOUR,
        durationMinutes = SLOT_DURATION_MINUTES,
        timeLabel = "$hour:00",
        durationLabel = "60 мин",
        status = status,
        clientDisplayName = client,
        hasPendingChangeRequest = false,
    )

private fun clientCardWithDynamics(): ClientCardState = ClientCardState.initial(clientUserId = "client-1").copy(
    checkIns = persistentListOf(
        clientCheckIn(id = "check-in-2", dateLabel = "24 August", measurements = "waist 78 cm"),
        clientCheckIn(id = "check-in-1", dateLabel = "1 August", measurements = "84 kg"),
    ),
    charts = persistentListOf(
        MetricChart(
            metric = ProgressMetric.Weight,
            title = "Weight",
            values = persistentListOf(84f, 83.2f, 82.4f),
            maxLabel = "84 kg",
            minLabel = "82 kg",
            rangeLabel = "1 August — 24 August",
            latestLabel = CLIENT_WEIGHT_LATEST,
            deltaLabel = CLIENT_WEIGHT_DELTA,
        ),
    ),
    selectedMetric = ProgressMetric.Weight,
    isLoading = false,
)

private fun clientCheckIn(id: String, dateLabel: String, measurements: String): CheckInRow = CheckInRow(
    checkInId = id,
    dateLabel = dateLabel,
    measurements = measurements,
    wellbeingLabel = "wellbeing 4",
    notes = null,
    review = CheckInReview.Awaiting,
    photos = persistentListOf(),
)

private fun progressWithCharts(): ProgressState = ProgressState.initial().copy(
    checkInDateLabel = "24 августа",
    checkInSummary = "82,4 кг · талия 78 см",
    hasCheckIn = true,
    charts = persistentListOf(
        MetricChart(
            metric = ProgressMetric.Weight,
            title = "Вес",
            values = persistentListOf(84f, 83.2f, 82.4f, 82.9f, 82.1f),
            maxLabel = "84 кг",
            minLabel = "82,1 кг",
            rangeLabel = "1 августа — 24 августа",
            latestLabel = "82,1 кг",
            deltaLabel = "−1,9 кг",
        ),
        MetricChart(
            metric = ProgressMetric.Waist,
            title = "Талия",
            values = persistentListOf(80f, 79f, 78f),
            maxLabel = "80 см",
            minLabel = "78 см",
            rangeLabel = "1 августа — 24 августа",
            latestLabel = "78 см",
            deltaLabel = "−2 см",
        ),
    ),
    selectedMetric = ProgressMetric.Weight,
    habits = persistentListOf(
        HabitRow(
            habitId = "habit-1",
            title = "10 000 шагов",
            isSetByCoach = true,
            doneCountLabel = "5 из 7",
            days = persistentListOf(*week().toTypedArray()),
        ),
    ),
    isLoading = false,
)

private fun week(): List<HabitDay> = (0 until 7).map { offset ->
    HabitDay(
        dateIso = LocalDate(MONDAY.year, MONDAY.month, MONDAY.day + offset).toString(),
        weekdayLabel = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")[offset],
        isDone = offset % 2 == 0,
        isToday = offset == 0,
        isFuture = offset > 3,
    )
}

private fun todayWithBlocks(): TodayState = TodayState.initial().copy(
    dateLabel = "чт 27.08",
    coachDisplayName = "Ляшук",
    sessions = persistentListOf(
        TodaySessionRow(
            slotId = "slot-1",
            clientUserId = "client-1",
            timeLabel = "12:00",
            durationLabel = "60 мин",
            clientDisplayName = "Анна",
            isNext = true,
            startsInLabel = "через 40 мин",
        ),
    ),
    unread = persistentListOf(
        TodayDialogRow(
            dialogId = "dialog-1",
            peerDisplayName = "Мария",
            preview = "Перенесём на завтра?",
            unreadCount = 2,
        ),
    ),
    moreUnreadCount = 4,
    lapsed = persistentListOf(
        TodayLapsedRow(userId = "client-2", displayName = "Ольга", since = LapsedSince.Days(11)),
        TodayLapsedRow(userId = "client-3", displayName = "Пётр", since = LapsedSince.Never),
    ),
    awaitingCheckIns = persistentListOf(
        TodayCheckInRow(
            checkInId = "check-in-1",
            clientUserId = "client-2",
            displayName = "Ольга",
            dateLabel = "26.08",
        ),
    ),
    tomorrow = TodayTomorrow.Sessions(summary = "2 тренировки · 09:30 · 18:00"),
    nextSession = TodayNextSession.Upcoming(
        dayLabel = "пт 28.08",
        timeLabel = "09:30",
        clientDisplayName = "Анна",
        startsInLabel = "через 21 ч",
    ),
    freeSlots = TodayFreeSlots.Available(summary = "Свободных слотов на неделе: 5"),
    isLoading = false,
)

private fun nextWithSession(): NextState = NextState.initial().copy(
    clientDisplayName = "Анна",
    session = NextSessionCard.Booked(
        dayLabel = "чт 27.08",
        timeLabel = "12:00",
        startsInLabel = "через 40 мин",
        coachDisplayName = "Ляшук",
        isToday = true,
        canRequestChange = true,
    ),
    fills = persistentListOf(
        FillRow(kind = FillKind.Diary, title = "Дневник тренировки", status = FillStatus.Pending),
        FillRow(kind = FillKind.CheckIn, title = "Замеры", status = FillStatus.Overdue(days = 18)),
    ),
    habits = persistentListOf(
        NextHabitRow(
            habitId = "habit-1",
            title = "10 000 шагов",
            isSetByCoach = true,
            doneCountLabel = "4/7",
            days = persistentListOf(
                HabitWeekDay.Done,
                HabitWeekDay.Done,
                HabitWeekDay.Missed,
                HabitWeekDay.Done,
                HabitWeekDay.Done,
                HabitWeekDay.Future,
                HabitWeekDay.Future,
            ),
        ),
    ),
    weekdayLabels = persistentListOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"),
    dynamics = NextDynamics.Weight(
        valueLabel = "82,1 кг",
        dateLabel = "26.08",
        deltaLabel = "−1,9 кг",
        isWeightDown = true,
        values = persistentListOf(84f, 83.2f, 82.4f, 82.9f, 82.1f),
        maxLabel = "84 кг",
        minLabel = "82,1 кг",
        rangeLabel = "28.07 — 26.08",
        measuresLabel = "78 см · 96 см · 94 см · 4 · 3",
    ),
    isLoading = false,
)

private fun programsWithOne(): ProgramsState = ProgramsState.initial().copy(
    programs = persistentListOf(
        ProgramRow(
            programId = "program-1",
            title = "Набор массы",
            summary = "4 нед · 3 дней заполнено · 2 назначено",
        ),
    ),
    isLoading = false,
)

private fun programEditorWithDays(): ProgramEditorState = ProgramEditorState.initial("program-1").copy(
    title = "Набор массы",
    weeksCount = 4,
    selectedWeek = 1,
    days = persistentListOf(
        DayRow(
            dayOfWeek = 1,
            label = "ПН",
            content = DayContent.Filled(title = "День ног", summary = "4 упр · 14 подходов"),
        ),
        DayRow(dayOfWeek = 2, label = "ВТ", content = DayContent.Empty),
        DayRow(
            dayOfWeek = 3,
            label = "СР",
            content = DayContent.Filled(title = "Верх тела", summary = "5 упр · 18 подходов"),
        ),
        DayRow(dayOfWeek = 4, label = "ЧТ", content = DayContent.Empty),
        DayRow(dayOfWeek = 5, label = "ПТ", content = DayContent.Empty),
        DayRow(dayOfWeek = 6, label = "СБ", content = DayContent.Empty),
        DayRow(dayOfWeek = 7, label = "ВС", content = DayContent.Empty),
    ),
    isLoading = false,
)

private fun programDayWithLines(): ProgramDayState = ProgramDayState
    .initial(programId = "program-1", weekNumber = 1, dayOfWeek = 1)
    .copy(
        dayLabel = "ПН",
        title = "День ног",
        lines = persistentListOf(
            ExerciseLineRow(
                exerciseId = "squat",
                exerciseName = "Приседания",
                setsText = "4",
                repetitionsText = "8",
                weightText = "60",
            ),
            ExerciseLineRow(
                exerciseId = "lunge",
                exerciseName = "Выпады",
                setsText = "3",
                repetitionsText = "12",
                weightText = "",
            ),
        ),
        choices = persistentListOf(
            ExerciseChoice(exerciseId = "squat", name = "Приседания"),
            ExerciseChoice(exerciseId = "lunge", name = "Выпады"),
            ExerciseChoice(exerciseId = "press", name = "Жим лёжа"),
        ),
        isLoading = false,
    )
