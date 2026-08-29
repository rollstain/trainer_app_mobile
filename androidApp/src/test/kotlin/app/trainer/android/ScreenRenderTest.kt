package app.trainer.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontFamily
import app.trainer.base.metrics.MetricChart
import app.trainer.base.metrics.ProgressMetric
import app.trainer.data.auth.AuthProvider
import app.trainer.data.clients.CoachPolicy
import app.trainer.data.schedule.SlotStatus
import app.trainer.feature.account.devices.mvi.DeviceRow
import app.trainer.feature.account.devices.mvi.DevicesState
import app.trainer.feature.account.devices.ui.DevicesView
import app.trainer.feature.account.identities.mvi.LoginMethodRow
import app.trainer.feature.account.identities.mvi.LoginMethodsState
import app.trainer.feature.account.identities.ui.LoginMethodsView
import app.trainer.feature.account.invite.mvi.InviteState
import app.trainer.feature.account.invite.ui.InviteView
import app.trainer.feature.account.invitelink.mvi.InviteLinkContent
import app.trainer.feature.account.invitelink.mvi.InviteLinkProblem
import app.trainer.feature.account.invitelink.mvi.InviteLinkState
import app.trainer.feature.account.invitelink.ui.InviteLinkView
import app.trainer.feature.account.nocoach.mvi.NoCoachState
import app.trainer.feature.account.nocoach.ui.NoCoachView
import app.trainer.feature.account.profile.mvi.ProfileState
import app.trainer.feature.account.profile.ui.ProfileView
import app.trainer.feature.account.welcome.mvi.TelegramLogin
import app.trainer.feature.account.welcome.mvi.WelcomeState
import app.trainer.feature.account.welcome.ui.WelcomeView
import app.trainer.feature.clientcard.presentation.mvi.CheckInReview
import app.trainer.feature.clientcard.presentation.mvi.CheckInRow
import app.trainer.feature.clientcard.presentation.mvi.ClientCardState
import app.trainer.feature.clientcard.presentation.mvi.ClientCardTab
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleState
import app.trainer.feature.clientcard.presentation.people.mvi.PersonRow
import app.trainer.feature.clientcard.presentation.people.ui.PeopleView
import app.trainer.feature.clientcard.presentation.ui.ClientCardView
import app.trainer.feature.home.presentation.next.mvi.FillKind
import app.trainer.feature.home.presentation.next.mvi.FillRow
import app.trainer.feature.home.presentation.next.mvi.FillStatus
import app.trainer.feature.home.presentation.next.mvi.NextBlock
import app.trainer.feature.home.presentation.next.mvi.NextDynamics
import app.trainer.feature.home.presentation.next.mvi.NextHabitRow
import app.trainer.feature.home.presentation.next.mvi.NextSessionCard
import app.trainer.feature.home.presentation.next.mvi.NextState
import app.trainer.feature.home.presentation.next.mvi.PlannedExerciseRow
import app.trainer.feature.home.presentation.next.mvi.PlannedToday
import app.trainer.feature.home.presentation.next.ui.NextView
import app.trainer.feature.home.presentation.today.mvi.LapsedSince
import app.trainer.feature.home.presentation.today.mvi.TodayBlock
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
import app.trainer.feature.progress.presentation.progress.mvi.ProgressBlock
import app.trainer.feature.progress.presentation.progress.mvi.ProgressPhotoRow
import app.trainer.feature.progress.presentation.progress.mvi.ProgressState
import app.trainer.feature.progress.presentation.progress.ui.ProgressView
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleDay
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleState
import app.trainer.feature.schedule.presentation.client.mvi.ClientSlotRow
import app.trainer.feature.schedule.presentation.client.mvi.CoachOption
import app.trainer.feature.schedule.presentation.client.ui.ClientScheduleView
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleState
import app.trainer.feature.schedule.presentation.coach.mvi.CoachSlotRow
import app.trainer.feature.schedule.presentation.coach.mvi.ScheduleDay
import app.trainer.feature.schedule.presentation.coach.mvi.SlotParticipantRow
import app.trainer.feature.schedule.presentation.coach.ui.CoachScheduleView
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupParticipantRow
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupSessionState
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupWaitingRow
import app.trainer.feature.schedule.presentation.groupsession.ui.GroupSessionView
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
import kotlinx.collections.immutable.persistentSetOf
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
private const val DEVICE_CURRENT = "THIS DEVICE"
private const val BLOCK_FAILED = "Could not load this"
private const val NEXT_HABITS_TITLE = "Habits"
private const val PROGRESS_HABITS_TITLE = "Habits"
private const val WELCOME_TITLE = "Training with your coach"
private const val WELCOME_TELEGRAM = "Sign in with Telegram"
private const val WELCOME_CODE = "I have a code from my coach"
private const val WELCOME_WAITING = "Waiting for Telegram…"
private const val LOGIN_METHODS_LINK = "Link Telegram"
private const val LOGIN_METHODS_LAST_HINT =
    "One way in must remain — otherwise you cannot get back into the account."
private const val NO_COACH_TITLE = "One thing left to choose"
private const val NO_COACH_COACH_ACTION = "I am a coach"
private const val NO_COACH_SIGN_OUT = "Sign out"
private const val GROUP_BOOKED = "booked"
private const val GROUP_FREE = "free"
private const val GROUP_WAITING = "waiting"
private const val GROUP_COMPLETE = "Mark as done"
private const val CHECK_INS_TITLE = "Check-ins without a reply"
private const val CODE_CASE_HINT = "Case does not matter"
private const val CODE_NOT_FOUND = "No such code. Check the message - 0 and O are easy to mix up."
private const val SESSION_EXPIRED_TITLE = "Sign in again"
private const val INVITE_LINK_TITLE = "An invitation from your coach"
private const val INVITE_LINK_JOIN = "Join"
private const val INVITE_LINK_EXPIRED = "The link has expired"
private const val INVITE_LINK_CODE_ACTION = "I have a code"
private const val DEVICE_REVOKE_OTHERS = "Sign out everywhere"
private const val DEVICE_RECOVERY_HINT =
    "Lost access to your account? Ask your coach for a new invite link: it brings back your diary and schedule."
private const val COMPARE_BEFORE = "Before"
private const val COMPARE_AFTER = "After"
private const val ATTENTION_REASON = "no diary entries for 12 days"
private const val GROUP_SEATS_LABEL = "3 of 8"
private const val GROUP_CAPACITY = 8
private const val PERSONAL_SEATS = 1
private const val GROUP_PARTICIPANTS = "Анна, Мария, Пётр"
private const val GROUP_FREE_SEATS = "5 of 8 seats free"
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
    fun `a client who stopped logging is marked with the reason`() {
        compose.setContent {
            TestTheme {
                PeopleView(state = peopleWithAttention(), onEvent = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(ATTENTION_REASON).assertIsDisplayed()
    }

    @Test
    fun `a group session shows who is coming and how full it is`() {
        compose.setContent {
            TestTheme {
                CoachScheduleView(state = coachScheduleWithGroupSession(), onEvent = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(GROUP_SEATS_LABEL).assertIsDisplayed()
        compose.onNodeWithText(GROUP_PARTICIPANTS).assertIsDisplayed()
    }

    @Test
    fun `a client sees how many seats are left`() {
        compose.setContent {
            TestTheme {
                ClientScheduleView(state = clientScheduleWithGroupSession(), onEvent = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(GROUP_FREE_SEATS).assertIsDisplayed()
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
    fun `on next a failed block keeps the rest of the screen alive`() {
        compose.setContent {
            TestTheme {
                NextView(state = nextWithFailedHabits(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(BLOCK_FAILED).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(NEXT_HABITS_TITLE).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `on progress a failed block does not replace the whole screen`() {
        compose.setContent {
            TestTheme {
                ProgressView(state = progressWithFailedCheckIn(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(BLOCK_FAILED).assertIsDisplayed()
        compose.onNodeWithText(PROGRESS_HABITS_TITLE).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the welcome screen offers telegram above the code`() {
        compose.setContent {
            TestTheme {
                WelcomeView(state = welcomeWithTelegram(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(WELCOME_TITLE).assertIsDisplayed()
        compose.onNodeWithText(WELCOME_TELEGRAM).assertIsDisplayed()
        compose.onNodeWithText(WELCOME_CODE).assertIsDisplayed()
    }

    @Test
    fun `while telegram is open the screen says what to do`() {
        compose.setContent {
            TestTheme {
                WelcomeView(state = welcomeWaitingForTelegram(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(WELCOME_WAITING).assertIsDisplayed()
    }

    @Test
    fun `the last way in cannot be unlinked`() {
        compose.setContent {
            TestTheme {
                LoginMethodsView(state = onlyTelegramLinked(), onEvent = {}, onBackClick = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(LOGIN_METHODS_LAST_HINT).assertIsDisplayed()
        compose.onNodeWithText(LOGIN_METHODS_LINK).assertDoesNotExist()
    }

    @Test
    fun `without a linked account the screen offers telegram`() {
        compose.setContent {
            TestTheme {
                LoginMethodsView(
                    state = LoginMethodsState.initial().copy(isLoading = false),
                    onEvent = {},
                    onBackClick = {},
                )
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(LOGIN_METHODS_LINK).assertIsDisplayed()
    }

    @Test
    fun `without a role the screen offers both the code and the coach request`() {
        compose.setContent {
            TestTheme {
                NoCoachView(state = NoCoachState.initial(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(NO_COACH_TITLE).assertIsDisplayed()
        compose.onNodeWithText(NO_COACH_COACH_ACTION).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(NO_COACH_SIGN_OUT).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a group session counts seats, the waitlist and offers to close it`() {
        compose.setContent {
            TestTheme {
                GroupSessionView(state = groupSession(), onEvent = {}, onBackClick = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(GROUP_BOOKED).assertIsDisplayed()
        compose.onNodeWithText(GROUP_FREE).assertIsDisplayed()
        compose.onNodeWithText(GROUP_WAITING).assertIsDisplayed()
        compose.onNodeWithText(GROUP_COMPLETE).assertIsDisplayed()
    }

    @Test
    fun `a block that did not answer keeps its title and offers its own retry`() {
        compose.setContent {
            TestTheme {
                TodayView(state = todayWithFailedCheckIns(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(CHECK_INS_TITLE).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(BLOCK_FAILED).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the code screen keeps the typed code when the code is unknown`() {
        compose.setContent {
            TestTheme {
                InviteView(state = codeNotFound(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(CODE_NOT_FOUND).assertIsDisplayed()
        compose.onNodeWithText(CODE_CASE_HINT).assertDoesNotExist()
    }

    @Test
    fun `an expired session asks to sign in again instead of showing an error`() {
        compose.setContent {
            TestTheme {
                InviteView(state = InviteState.initial(afterSessionExpiry = true), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(SESSION_EXPIRED_TITLE).assertIsDisplayed()
    }

    @Test
    fun `the invite link shows the coach and what the coach will see`() {
        compose.setContent {
            TestTheme {
                InviteLinkView(state = inviteLink(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(INVITE_LINK_TITLE).assertIsDisplayed()
        compose.onNodeWithText(COACH_NAME).assertIsDisplayed()
        compose.onNodeWithText(INVITE_LINK_JOIN).assertIsDisplayed()
    }

    @Test
    fun `an expired link offers the code instead of a retry`() {
        compose.setContent {
            TestTheme {
                InviteLinkView(state = expiredLink(), onEvent = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(INVITE_LINK_EXPIRED).assertIsDisplayed()
        compose.onNodeWithText(INVITE_LINK_CODE_ACTION).assertIsDisplayed()
    }

    @Test
    fun `the devices screen marks the current device and offers to sign the others out`() {
        compose.setContent {
            TestTheme {
                DevicesView(state = devices(), onEvent = {}, onBackClick = {})
            }
        }

        compose.waitForIdle()

        compose.onNodeWithText(DEVICE_CURRENT).assertIsDisplayed()
        compose.onNodeWithText(DEVICE_REVOKE_OTHERS).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(DEVICE_RECOVERY_HINT).performScrollTo().assertExists()
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

        compose.onNodeWithText(CLIENT_WEIGHT_LATEST).assertExists()
        compose.onNodeWithText(CLIENT_WEIGHT_DELTA).assertExists()
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

private const val COACH_NAME = "Dmitry Rogov"

private fun onlyTelegramLinked(): LoginMethodsState = LoginMethodsState.initial().copy(
    isLoading = false,
    methods = persistentListOf(
        LoginMethodRow(provider = AuthProvider.TELEGRAM, linkedAtLabel = "linked on 29.08"),
    ),
)

private fun welcomeWithTelegram(): WelcomeState = WelcomeState.initial(afterSessionExpiry = false)

private fun welcomeWaitingForTelegram(): WelcomeState = welcomeWithTelegram().copy(
    telegram = TelegramLogin.Waiting,
)

private fun nextWithFailedHabits(): NextState = NextState.initial().copy(
    isLoading = false,
    clientDisplayName = "Anna",
    session = NextSessionCard.NoSlots,
    failedBlocks = persistentSetOf(NextBlock.Habits),
)

private fun progressWithFailedCheckIn(): ProgressState = ProgressState.initial().copy(
    isLoading = false,
    failedBlocks = persistentSetOf(ProgressBlock.CheckIn),
)

private fun groupSession(): GroupSessionState = GroupSessionState.initial().copy(
    isLoading = false,
    title = "Group session",
    whenLabel = "tue 25.08 · 12:00—13:30",
    takenSeats = 2,
    freeSeats = 6,
    participants = persistentListOf(
        GroupParticipantRow(
            clientUserId = "client-1",
            displayName = "Sergey Panov",
            bookedAtLabel = "booked on 22.08",
            hasMedicalNotes = true,
        ),
        GroupParticipantRow(
            clientUserId = "client-2",
            displayName = "Elena Litvinova",
            bookedAtLabel = "booked on 22.08",
            hasMedicalNotes = false,
        ),
    ),
    waiting = persistentListOf(
        GroupWaitingRow(
            clientUserId = "client-3",
            displayName = "Pavel Kim",
            joinedAtLabel = "booked on 25.08",
        ),
    ),
)

private fun todayWithFailedCheckIns(): TodayState = TodayState.initial().copy(
    isLoading = false,
    dateLabel = "tuesday, 26 august",
    coachDisplayName = "Dmitry",
    failedBlocks = persistentSetOf(TodayBlock.CheckIns),
)

private fun codeNotFound(): InviteState = InviteState.initial(afterSessionExpiry = false).copy(
    code = "K7M3Q9",
    codeError = CODE_NOT_FOUND,
)

private fun inviteLink(): InviteLinkState = InviteLinkState.initial().copy(
    content = InviteLinkContent.Coach(displayName = COACH_NAME, needsDisplayName = true),
)

private fun expiredLink(): InviteLinkState = InviteLinkState.initial().copy(
    content = InviteLinkContent.Problem(InviteLinkProblem.Expired),
)

private fun devices(): DevicesState = DevicesState.initial().copy(
    isLoading = false,
    devices = persistentListOf(
        DeviceRow(
            sessionId = "session-1",
            deviceInfo = "Pixel 7a",
            lastSeenLabel = "28.08 09:12",
            isLongUnused = false,
            isCurrent = true,
        ),
        DeviceRow(
            sessionId = "session-2",
            deviceInfo = "iPhone 13",
            lastSeenLabel = "12.05 20:40",
            isLongUnused = true,
            isCurrent = false,
        ),
    ),
)

private fun peopleWithAttention(): PeopleState = PeopleState.initial().withFirstPage(
    booked = listOf(),
    others = listOf(
        PersonRow(
            userId = "client-1",
            displayName = "Сергей Панов",
            hasMedicalNotes = false,
            nextSessionLabel = null,
            hasPendingChangeRequest = false,
            unreadCount = 0,
            attentionReason = ATTENTION_REASON,
        ),
    ),
    nextCursor = null,
)

private fun coachScheduleWithGroupSession(): CoachScheduleState = CoachScheduleState.initial().copy(
    weekStart = MONDAY,
    weekTitle = "24—30 августа",
    days = persistentListOf(
        ScheduleDay(
            date = MONDAY,
            weekdayLabel = "ПН",
            dayNumberLabel = MONDAY.day.toString(),
            isToday = true,
            isWeekend = false,
            slots = persistentListOf(
                slotAt(
                    hour = 19,
                    index = 0,
                    status = SlotStatus.FREE,
                    client = null,
                    seats = SlotSeats.Group(label = GROUP_SEATS_LABEL, names = GROUP_PARTICIPANTS),
                ),
            ),
        ),
    ),
    isLoading = false,
)

private fun clientScheduleWithGroupSession(): ClientScheduleState = ClientScheduleState.initial().copy(
    coaches = persistentListOf(CoachOption(coachId = "coach-1", displayName = "Тренер")),
    selectedCoachId = "coach-1",
    weekStart = MONDAY,
    selectedDate = MONDAY,
    days = persistentListOf(
        ClientScheduleDay(
            date = MONDAY,
            weekdayLabel = "ПН",
            dayNumberLabel = MONDAY.day.toString(),
            isToday = true,
            isWeekend = false,
            slots = persistentListOf(
                ClientSlotRow(
                    slotId = "slot-group",
                    timeLabel = "19:00",
                    durationLabel = "60 мин",
                    isBookedByMe = false,
                    isAvailable = true,
                    hasPendingChangeRequest = false,
                    canRequestChange = false,
                    isOnWaitlist = false,
                    note = "",
                    seatsLabel = GROUP_FREE_SEATS,
                ),
            ),
        ),
    ),
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

private fun slotAt(
    hour: Int,
    index: Int,
    status: SlotStatus,
    client: String?,
    seats: SlotSeats = SlotSeats.Personal,
): CoachSlotRow =
    CoachSlotRow(
        slotId = "slot-$hour-$index",
        startMinutesOfDay = hour * MINUTES_IN_HOUR,
        durationMinutes = SLOT_DURATION_MINUTES,
        timeLabel = "$hour:00",
        durationLabel = "60 мин",
        status = status,
        clientDisplayName = client,
        hasPendingChangeRequest = false,
        isGroup = seats is SlotSeats.Group,
        hasParticipants = client != null || seats is SlotSeats.Group,
        capacity = (seats as? SlotSeats.Group)?.capacity ?: PERSONAL_SEATS,
        takenSeats = if (client == null && seats is SlotSeats.Personal) 0 else 1,
        participants = client
            ?.let { persistentListOf(SlotParticipantRow(clientUserId = "client-1", displayName = it)) }
            ?: persistentListOf(),
        seatsLabel = (seats as? SlotSeats.Group)?.label.orEmpty(),
        participantNames = (seats as? SlotSeats.Group)?.names.orEmpty(),
    )

private sealed interface SlotSeats {

    data object Personal : SlotSeats

    data class Group(val label: String, val names: String, val capacity: Int = GROUP_CAPACITY) : SlotSeats
}

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
    tab = ClientCardTab.Metrics,
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
            seatsLabel = "",
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
