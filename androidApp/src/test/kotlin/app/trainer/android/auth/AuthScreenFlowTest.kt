package app.trainer.android.auth

import app.trainer.data.auth.FreshSignUp
import app.trainer.entities.RequestFailure
import app.trainer.feature.account.newpassword.mvi.LinkState
import app.trainer.feature.account.newpassword.mvi.NewPasswordEvent
import app.trainer.feature.account.newpassword.mvi.NewPasswordScreenModel
import app.trainer.feature.account.recovery.mvi.RecoveryEvent
import app.trainer.feature.account.recovery.mvi.RecoveryScreenModel
import app.trainer.feature.account.recovery.mvi.RecoveryStep
import app.trainer.feature.account.signin.mvi.SignInEvent
import app.trainer.feature.account.signin.mvi.SignInFailure
import app.trainer.feature.account.signin.mvi.SignInScreenModel
import app.trainer.feature.account.signup.mvi.LoginField
import app.trainer.feature.account.signup.mvi.SignUpEvent
import app.trainer.feature.account.signup.mvi.SignUpScreenModel
import app.trainer.feature.account.telegram.TelegramConfirmation
import app.trainer.logger.ConsoleLogger
import app.trainer.logger.Logger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val DEVICE = "Pixel 8"
private const val EMAIL = "anna@mail.ru"
private const val PASSWORD = "gantel-2026"
private const val LOCK_SECONDS = 300L
private const val RESEND_SECONDS = 120L
private const val MILLIS_IN_SECOND = 1000L
private const val PAST_THE_TICK = 1L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = android.app.Application::class,
    qualifiers = "ru-rRU-w411dp-h891dp-xhdpi",
)
class AuthScreenFlowTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val auth = FakeAuthRepository()
    private val freshSignUp = FreshSignUp()

    private val telegram = TelegramConfirmation(
        authRepository = auth,
        identitiesRepository = auth,
        deviceInfo = DEVICE,
    )

    @Before
    fun start() {
        Dispatchers.setMain(dispatcher)
        startKoin { modules(module { single<Logger> { ConsoleLogger() } }) }
    }

    @After
    fun stop() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `a locked sign-in counts down and opens by itself`() = runTest(dispatcher.scheduler) {
        auth.signInAnswer = failure(RequestFailure.TooManyRequests, retryAfterSeconds = LOCK_SECONDS)
        val model = signInModel()
        model.dispatch(SignInEvent.OnIdentifierChanged(EMAIL))
        model.dispatch(SignInEvent.OnPasswordChanged("wrong"))

        model.dispatch(SignInEvent.OnSubmitClicked)
        runCurrent()
        assertEquals(LOCK_SECONDS, (model.stateChanges.value.failure as SignInFailure.Locked).secondsLeft)

        advanceTimeBy((LOCK_SECONDS - 1) * MILLIS_IN_SECOND + PAST_THE_TICK)
        assertEquals(1L, (model.stateChanges.value.failure as SignInFailure.Locked).secondsLeft, "счётчик тикает вслух")

        advanceTimeBy(MILLIS_IN_SECOND + PAST_THE_TICK)
        assertEquals(SignInFailure.None, model.stateChanges.value.failure, "по истечении срока экран открывается сам")
    }

    @Test
    fun `a wrong pair keeps what the person typed`() = runTest(dispatcher.scheduler) {
        auth.signInAnswer = failure(RequestFailure.Unauthorized)
        val model = signInModel()
        model.dispatch(SignInEvent.OnIdentifierChanged(EMAIL))
        model.dispatch(SignInEvent.OnPasswordChanged(PASSWORD))

        model.dispatch(SignInEvent.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals(SignInFailure.Rejected, model.stateChanges.value.failure)
        assertEquals(PASSWORD, model.stateChanges.value.password, "стирать набранное — причина третьей неудачи подряд")
        assertEquals(EMAIL, model.stateChanges.value.identifier)
    }

    @Test
    fun `no network is offered as a retry, not as a wrong password`() = runTest(dispatcher.scheduler) {
        auth.signInAnswer = failure(RequestFailure.Network)
        val model = signInModel()
        model.dispatch(SignInEvent.OnIdentifierChanged(EMAIL))
        model.dispatch(SignInEvent.OnPasswordChanged(PASSWORD))

        model.dispatch(SignInEvent.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals(SignInFailure.Offline, model.stateChanges.value.failure)
    }

    @Test
    fun `a successful sign-in leaves a session`() = runTest(dispatcher.scheduler) {
        val model = signInModel()
        model.dispatch(SignInEvent.OnIdentifierChanged(EMAIL))
        model.dispatch(SignInEvent.OnPasswordChanged(PASSWORD))

        model.dispatch(SignInEvent.OnSubmitClicked)
        advanceUntilIdle()

        assertTrue(auth.isSignedIn)
        assertEquals(SignInFailure.None, model.stateChanges.value.failure)
    }

    @Test
    fun `sign-up remembers the name so the telegram offer can greet`() = runTest(dispatcher.scheduler) {
        val model = signUpModel()
        model.dispatch(SignUpEvent.OnNameChanged("Анна"))
        model.dispatch(SignUpEvent.OnEmailChanged(EMAIL))
        model.dispatch(SignUpEvent.OnPasswordChanged(PASSWORD))

        model.dispatch(SignUpEvent.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals("Анна", freshSignUp.name.value)
        assertTrue(auth.isSignedIn)
    }

    @Test
    fun `a taken email is shown at the email field`() = runTest(dispatcher.scheduler) {
        auth.signUpAnswer = failure(RequestFailure.Conflict, field = "email")
        val model = signUpModel()
        model.dispatch(SignUpEvent.OnNameChanged("Анна"))
        model.dispatch(SignUpEvent.OnEmailChanged(EMAIL))
        model.dispatch(SignUpEvent.OnPasswordChanged(PASSWORD))

        model.dispatch(SignUpEvent.OnSubmitClicked)
        advanceUntilIdle()

        assertNotNull(model.stateChanges.value.emailError)
        assertNull(freshSignUp.name.value, "провалившаяся регистрация не должна вести на предложение Telegram")
    }

    @Test
    fun `a taken login offers a replacement next to the field`() = runTest(dispatcher.scheduler) {
        auth.signUpAnswer = failure(RequestFailure.Conflict, field = "login")
        val model = signUpModel()
        model.dispatch(SignUpEvent.OnNameChanged("Анна"))
        model.dispatch(SignUpEvent.OnEmailChanged(EMAIL))
        model.dispatch(SignUpEvent.OnLoginRequested)
        model.dispatch(SignUpEvent.OnLoginChanged("anna"))
        model.dispatch(SignUpEvent.OnPasswordChanged(PASSWORD))

        model.dispatch(SignUpEvent.OnSubmitClicked)
        advanceUntilIdle()

        val login = model.stateChanges.value.login as LoginField.Shown
        assertEquals("anna", login.value, "введённый логин не стирается")
        assertTrue(login.error.orEmpty().contains("anna_1"), "подсказка называет свободный вариант: ${login.error}")
    }

    @Test
    fun `the password hint counts out loud before the request`() = runTest(dispatcher.scheduler) {
        val model = signUpModel()

        model.dispatch(SignUpEvent.OnPasswordChanged("gant"))
        assertEquals(4, model.stateChanges.value.charsMissing)

        model.dispatch(SignUpEvent.OnPasswordChanged(PASSWORD))
        assertEquals(0, model.stateChanges.value.charsMissing)
        assertTrue(model.stateChanges.value.password.length > 8)
    }

    @Test
    fun `a sent letter closes the resend for two minutes and opens it back`() = runTest(dispatcher.scheduler) {
        val model = recoveryModel()

        model.dispatch(RecoveryEvent.OnSendClicked)
        runCurrent()

        assertEquals(RESEND_SECONDS, (model.stateChanges.value.step as RecoveryStep.LetterSent).resendSecondsLeft)
        assertTrue(!model.stateChanges.value.isSendEnabled, "пока идёт отсчёт, повтор закрыт")

        advanceTimeBy(RESEND_SECONDS * MILLIS_IN_SECOND + PAST_THE_TICK)
        assertTrue(model.stateChanges.value.isSendEnabled, "через две минуты письмо можно попросить снова")
    }

    @Test
    fun `a refused letter says so and keeps the telegram way open`() = runTest(dispatcher.scheduler) {
        auth.forgotAnswer = failure(RequestFailure.Server)
        val model = recoveryModel()

        model.dispatch(RecoveryEvent.OnSendClicked)
        advanceUntilIdle()

        assertEquals(RecoveryStep.LetterRefused, model.stateChanges.value.step)
        assertTrue(model.stateChanges.value.isSendEnabled, "повторить отправку можно сразу — это не запрет, а отказ")
    }

    @Test
    fun `a used link and an expired one land on different screens`() = runTest(dispatcher.scheduler) {
        auth.resetByEmailAnswers = ArrayDeque(
            listOf(failure(RequestFailure.Conflict), failure(RequestFailure.Gone)),
        )

        val used = newPasswordModel(resetToken = "used")
        used.dispatch(NewPasswordEvent.OnPasswordChanged(PASSWORD))
        used.dispatch(NewPasswordEvent.OnSubmitClicked)
        advanceUntilIdle()
        assertEquals(LinkState.AlreadyUsed, used.stateChanges.value.link)

        val expired = newPasswordModel(resetToken = "old")
        expired.dispatch(NewPasswordEvent.OnPasswordChanged(PASSWORD))
        expired.dispatch(NewPasswordEvent.OnSubmitClicked)
        advanceUntilIdle()
        assertEquals(LinkState.Expired, expired.stateChanges.value.link)
    }

    @Test
    fun `a new password from the letter opens a session`() = runTest(dispatcher.scheduler) {
        val model = newPasswordModel(resetToken = "fresh")

        model.dispatch(NewPasswordEvent.OnPasswordChanged(PASSWORD))
        model.dispatch(NewPasswordEvent.OnSubmitClicked)
        advanceUntilIdle()

        assertTrue(auth.isSignedIn)
        assertEquals(LinkState.Usable, model.stateChanges.value.link)
    }

    @Test
    fun `a telegram claim that nobody confirmed asks to try again`() = runTest(dispatcher.scheduler) {
        auth.resetByTelegramAnswer = failure(RequestFailure.Conflict)
        val model = newPasswordModel(resetToken = null, claimToken = "claim")

        model.dispatch(NewPasswordEvent.OnPasswordChanged(PASSWORD))
        model.dispatch(NewPasswordEvent.OnSubmitClicked)
        advanceUntilIdle()

        assertEquals(LinkState.NotConfirmedYet, model.stateChanges.value.link)
        assertTrue(!auth.isSignedIn)
    }

    private fun signInModel() = SignInScreenModel(
        authRepository = auth,
        telegramConfirmation = telegram,
        deviceInfo = DEVICE,
    )

    private fun signUpModel() = SignUpScreenModel(
        authRepository = auth,
        freshSignUp = freshSignUp,
        deviceInfo = DEVICE,
    )

    private fun recoveryModel() = RecoveryScreenModel(
        email = EMAIL,
        authRepository = auth,
        telegramConfirmation = telegram,
    )

    private fun newPasswordModel(resetToken: String?, claimToken: String? = null) = NewPasswordScreenModel(
        resetToken = resetToken,
        claimToken = claimToken,
        authRepository = auth,
        deviceInfo = DEVICE,
    )
}
