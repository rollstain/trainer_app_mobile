package app.trainer.feature.account.welcome.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.feature.account.telegram.TelegramConfirmation
import app.trainer.feature.account.telegram.TelegramLoginState
import kotlinx.coroutines.Job

class WelcomeScreenModel(
    afterSessionExpiry: Boolean,
    private val telegramConfirmation: TelegramConfirmation,
) : BaseScreenModel<WelcomeState, WelcomeSideEffect, WelcomeEvent>(
    initialState = WelcomeState.initial(afterSessionExpiry = afterSessionExpiry),
) {

    private var telegramJob: Job? = null

    override fun onFetchData() = Unit

    override fun dispatch(event: WelcomeEvent) {
        when (event) {
            WelcomeEvent.OnTelegramClicked -> startTelegram()
            WelcomeEvent.OnTelegramCancelled -> cancelTelegram()
            WelcomeEvent.OnSignInClicked -> screenModelScope { postSideEffect(WelcomeSideEffect.OpenSignIn) }
            WelcomeEvent.OnSignUpClicked -> screenModelScope { postSideEffect(WelcomeSideEffect.OpenSignUp) }
            WelcomeEvent.OnCodeClicked -> screenModelScope { postSideEffect(WelcomeSideEffect.OpenCodeEntry) }
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
                is RequestResult.Error -> showTelegramFailure(started)
                is RequestResult.Success -> {
                    updateState {
                        it.copy(telegram = TelegramLoginState.Waiting(telegramConfirmation.confirmationWindowSeconds))
                    }
                    postSideEffect(WelcomeSideEffect.OpenTelegram(deepLink = started.data.deepLink))
                    awaitSignIn(claimToken = started.data.claimToken)
                }
            }
        }
    }

    private suspend fun awaitSignIn(claimToken: String) {
        val confirmed = telegramConfirmation.awaitSignIn(claimToken = claimToken) { secondsLeft ->
            updateState { it.copy(telegram = TelegramLoginState.Waiting(secondsLeft = secondsLeft)) }
        }
        if (confirmed is RequestResult.Error) showTelegramFailure(confirmed)
    }

    private fun showTelegramFailure(failure: RequestResult.Error) {
        updateState {
            it.copy(telegram = TelegramLoginState.Failed(isExpired = failure.kind == RequestFailure.Gone))
        }
    }
}
