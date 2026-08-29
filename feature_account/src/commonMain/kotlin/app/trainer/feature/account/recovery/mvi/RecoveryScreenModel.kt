package app.trainer.feature.account.recovery.mvi

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
private const val RESEND_WAIT_SECONDS = 120L

class RecoveryScreenModel(
    email: String,
    private val authRepository: AuthRepository,
    private val telegramConfirmation: TelegramConfirmation,
) : BaseScreenModel<RecoveryState, RecoverySideEffect, RecoveryEvent>(
    initialState = RecoveryState.initial(email = email),
) {

    private var countdownJob: Job? = null
    private var telegramJob: Job? = null

    override fun onFetchData() = Unit

    override fun dispatch(event: RecoveryEvent) {
        when (event) {
            is RecoveryEvent.OnEmailChanged -> updateState { it.copy(email = event.value) }
            RecoveryEvent.OnSendClicked -> send()
            RecoveryEvent.OnTelegramClicked -> startTelegram()
            RecoveryEvent.OnTelegramCancelled -> cancelTelegram()
            RecoveryEvent.OnTelegramConfirmed -> continueWithTelegram()
            RecoveryEvent.OnCodeClicked -> screenModelScope { postSideEffect(RecoverySideEffect.OpenCodeEntry) }
        }
    }

    private fun send() {
        screenModelScope { current ->
            if (!current.isSendEnabled) return@screenModelScope
            updateState { it.copy(isSending = true) }
            val asked = authRepository.requestPasswordReset(email = current.email)
            updateState { it.copy(isSending = false) }
            when (asked) {
                is RequestResult.Success -> letterSent()
                is RequestResult.Error -> showSendFailure(asked)
            }
        }
    }

    private fun letterSent() {
        countdownJob?.cancel()
        updateState { it.copy(step = RecoveryStep.LetterSent(resendSecondsLeft = RESEND_WAIT_SECONDS)) }
        countdownJob = screenModelScope {
            var left = RESEND_WAIT_SECONDS
            while (left > RecoveryState.NO_WAIT) {
                delay(COUNTDOWN_STEP)
                left -= COUNTDOWN_STEP.inWholeSeconds
                updateState { it.copy(step = RecoveryStep.LetterSent(resendSecondsLeft = left)) }
            }
        }
    }

    private suspend fun showSendFailure(failure: RequestResult.Error) {
        when (failure.kind) {
            RequestFailure.Server, RequestFailure.Validation ->
                updateState { it.copy(step = RecoveryStep.LetterRefused) }
            else -> postSideEffect(RecoverySideEffect.ShowFailure(failure))
        }
    }

    private fun cancelTelegram() {
        telegramJob?.cancel()
        telegramJob = null
        updateState { it.copy(telegram = TelegramLoginState.Idle, claimToken = null) }
    }

    private fun continueWithTelegram() {
        screenModelScope { current ->
            val token = current.claimToken ?: return@screenModelScope
            postSideEffect(RecoverySideEffect.OpenNewPassword(claimToken = token))
        }
    }

    private fun startTelegram() {
        telegramJob?.cancel()
        telegramJob = screenModelScope {
            updateState { it.copy(telegram = TelegramLoginState.Starting) }
            when (val started = telegramConfirmation.start()) {
                is RequestResult.Error -> showTelegramFailure(started)
                is RequestResult.Success -> {
                    updateState {
                        it.copy(
                            telegram = TelegramLoginState.Waiting(telegramConfirmation.confirmationWindowSeconds),
                            claimToken = started.data.claimToken,
                        )
                    }
                    postSideEffect(RecoverySideEffect.OpenTelegram(deepLink = started.data.deepLink))
                    countDownConfirmation()
                }
            }
        }
    }

    private suspend fun countDownConfirmation() {
        var left = telegramConfirmation.confirmationWindowSeconds
        while (left > RecoveryState.NO_WAIT) {
            delay(COUNTDOWN_STEP)
            left -= COUNTDOWN_STEP.inWholeSeconds
            updateState { it.copy(telegram = TelegramLoginState.Waiting(secondsLeft = left)) }
        }
        updateState { it.copy(telegram = TelegramLoginState.Failed(isExpired = true), claimToken = null) }
    }

    private fun showTelegramFailure(failure: RequestResult.Error) {
        updateState {
            it.copy(telegram = TelegramLoginState.Failed(isExpired = failure.kind == RequestFailure.Gone))
        }
    }
}
