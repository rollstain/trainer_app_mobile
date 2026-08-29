package app.trainer.feature.account.welcome.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthProvider
import app.trainer.data.auth.AuthRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.welcome_telegram_expired
import app.trainer.strings.welcome_telegram_failed
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString

private val CONFIRMATION_POLL_DELAY = 2.seconds
private const val CONFIRMATION_ATTEMPTS = 60

class WelcomeScreenModel(
    afterSessionExpiry: Boolean,
    private val authRepository: AuthRepository,
    private val deviceInfo: String,
) : BaseScreenModel<WelcomeState, WelcomeSideEffect, WelcomeEvent>(
    initialState = WelcomeState.initial(afterSessionExpiry = afterSessionExpiry),
) {

    private var telegramJob: Job? = null

    override fun onFetchData() = Unit

    override fun dispatch(event: WelcomeEvent) {
        when (event) {
            WelcomeEvent.OnTelegramClicked -> startTelegram(intent = LoginIntent.Client)
            WelcomeEvent.OnCoachClicked -> startTelegram(intent = LoginIntent.Coach)
            WelcomeEvent.OnTelegramCancelled -> cancelTelegram()
            WelcomeEvent.OnCodeClicked -> screenModelScope {
                postSideEffect(WelcomeSideEffect.OpenCodeEntry)
            }
        }
    }

    private fun cancelTelegram() {
        telegramJob?.cancel()
        telegramJob = null
        updateState { it.copy(telegram = TelegramLogin.Idle) }
    }

    private fun startTelegram(intent: LoginIntent) {
        telegramJob?.cancel()
        telegramJob = screenModelScope {
            updateState { it.copy(telegram = TelegramLogin.Starting) }
            when (val started = authRepository.startTelegramLogin()) {
                is RequestResult.Error -> showTelegramFailure(started)
                is RequestResult.Success -> {
                    updateState { it.copy(telegram = TelegramLogin.Waiting) }
                    postSideEffect(WelcomeSideEffect.OpenTelegram(deepLink = started.data.deepLink))
                    awaitConfirmation(claimToken = started.data.claimToken, intent = intent)
                }
            }
        }
    }

    private suspend fun awaitConfirmation(claimToken: String, intent: LoginIntent) {
        repeat(CONFIRMATION_ATTEMPTS) {
            delay(CONFIRMATION_POLL_DELAY)
            val signedIn = authRepository.signInWithProvider(
                provider = AuthProvider.TELEGRAM,
                token = claimToken,
                deviceInfo = deviceInfo,
            )
            when {
                signedIn is RequestResult.Success -> {
                    if (intent == LoginIntent.Coach) askCoachAccess()
                    return
                }
                signedIn is RequestResult.Error && signedIn.kind == RequestFailure.Conflict -> Unit
                signedIn is RequestResult.Error -> {
                    showTelegramFailure(signedIn)
                    return
                }
            }
        }
        val expired = getString(Res.string.welcome_telegram_expired)
        updateState { it.copy(telegram = TelegramLogin.Failed(expired)) }
    }

    private suspend fun askCoachAccess() {
        when (val asked = authRepository.askCoachAccess()) {
            is RequestResult.Error -> postSideEffect(WelcomeSideEffect.ShowFailure(asked))
            is RequestResult.Success -> postSideEffect(WelcomeSideEffect.ShowCoachRequested)
        }
    }

    private suspend fun showTelegramFailure(failure: RequestResult.Error) {
        val message = when (failure.kind) {
            RequestFailure.Gone -> getString(Res.string.welcome_telegram_expired)
            else -> getString(Res.string.welcome_telegram_failed)
        }
        updateState { it.copy(telegram = TelegramLogin.Failed(message)) }
    }
}
