package app.trainer.feature.account.signin.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.feature.account.telegram.TelegramConfirmation
import app.trainer.feature.account.telegram.TelegramLoginState
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

private val COUNTDOWN_STEP = 1.seconds
private const val NO_SECONDS_LEFT = 0L

class SignInScreenModel(
    private val authRepository: AuthRepository,
    private val telegramConfirmation: TelegramConfirmation,
    private val deviceInfo: String,
) : BaseScreenModel<SignInState, SignInSideEffect, SignInEvent>(
    initialState = SignInState.initial(),
) {

    private var countdownJob: Job? = null
    private var telegramJob: Job? = null

    override fun onFetchData() = Unit

    override fun dispatch(event: SignInEvent) {
        when (event) {
            is SignInEvent.OnIdentifierChanged -> updateState {
                it.copy(identifier = event.value, failure = clearedFailure(it.failure))
            }
            is SignInEvent.OnPasswordChanged -> updateState {
                it.copy(password = event.value, failure = clearedFailure(it.failure))
            }
            SignInEvent.OnRevealToggled -> updateState { it.copy(isPasswordRevealed = !it.isPasswordRevealed) }
            SignInEvent.OnSubmitClicked -> submit()
            SignInEvent.OnForgotClicked -> screenModelScope { current ->
                postSideEffect(SignInSideEffect.OpenRecovery(email = current.identifier.trim()))
            }
            SignInEvent.OnTelegramClicked -> startTelegram()
            SignInEvent.OnTelegramCancelled -> cancelTelegram()
        }
    }

    private fun clearedFailure(failure: SignInFailure): SignInFailure =
        if (failure is SignInFailure.Locked) failure else SignInFailure.None

    private fun submit() {
        screenModelScope { current ->
            if (!current.isSubmitEnabled) return@screenModelScope
            updateState { it.copy(isSubmitting = true, failure = SignInFailure.None) }
            val signedIn = authRepository.signInWithPassword(
                identifier = current.identifier,
                password = current.password,
                deviceInfo = deviceInfo,
            )
            updateState { it.copy(isSubmitting = false) }
            when (signedIn) {
                is RequestResult.Success -> postSideEffect(SignInSideEffect.SignedIn)
                is RequestResult.Error -> showFailure(signedIn)
            }
        }
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        when (failure.kind) {
            RequestFailure.Unauthorized -> updateState { it.copy(failure = SignInFailure.Rejected) }
            RequestFailure.Network -> updateState { it.copy(failure = SignInFailure.Offline) }
            RequestFailure.TooManyRequests -> lockUntil(failure.retryAfterSeconds ?: NO_SECONDS_LEFT)
            else -> postSideEffect(SignInSideEffect.ShowFailure(failure))
        }
    }

    private fun lockUntil(secondsLeft: Long) {
        countdownJob?.cancel()
        updateState { it.copy(failure = SignInFailure.Locked(secondsLeft = secondsLeft)) }
        countdownJob = screenModelScope {
            var left = secondsLeft
            while (left > NO_SECONDS_LEFT) {
                delay(COUNTDOWN_STEP)
                left -= COUNTDOWN_STEP.inWholeSeconds
                updateState { it.copy(failure = SignInFailure.Locked(secondsLeft = left)) }
            }
            updateState { it.copy(failure = SignInFailure.None) }
        }
    }

    private fun cancelTelegram() {
        telegramJob?.cancel()
        telegramJob = null
        updateState { it.copy(telegram = TelegramLoginState.Idle) }
    }

    private fun startTelegram() {
        telegramJob?.cancel()
        telegramJob = screenModelScope {
            updateState { it.copy(telegram = TelegramLoginState.Starting) }
            when (val started = telegramConfirmation.start()) {
                is RequestResult.Error -> updateState {
                    it.copy(telegram = TelegramLoginState.Failed(isExpired = started.kind == RequestFailure.Gone))
                }
                is RequestResult.Success -> {
                    updateState {
                        it.copy(telegram = TelegramLoginState.Waiting(telegramConfirmation.confirmationWindowSeconds))
                    }
                    postSideEffect(SignInSideEffect.OpenTelegram(deepLink = started.data.deepLink))
                    awaitTelegram(claimToken = started.data.claimToken)
                }
            }
        }
    }

    private suspend fun awaitTelegram(claimToken: String) {
        val confirmed = telegramConfirmation.awaitSignIn(claimToken = claimToken) { secondsLeft ->
            updateState { it.copy(telegram = TelegramLoginState.Waiting(secondsLeft = secondsLeft)) }
        }
        when (confirmed) {
            is RequestResult.Success -> postSideEffect(SignInSideEffect.SignedIn)
            is RequestResult.Error -> updateState {
                it.copy(telegram = TelegramLoginState.Failed(isExpired = confirmed.kind == RequestFailure.Gone))
            }
        }
    }
}
