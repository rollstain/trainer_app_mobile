package app.trainer.feature.account.telegram

import app.trainer.data.auth.AuthProvider
import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.data.auth.TelegramLoginStart
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

private val CONFIRMATION_POLL_DELAY = 3.seconds
private val CONFIRMATION_WINDOW = 15.minutes

sealed interface TelegramLoginState {

    data object Idle : TelegramLoginState

    data object Starting : TelegramLoginState

    data class Waiting(val secondsLeft: Long) : TelegramLoginState

    data class Failed(val isExpired: Boolean) : TelegramLoginState
}

class TelegramConfirmation(
    private val authRepository: AuthRepository,
    private val identitiesRepository: IdentitiesRepository,
    private val deviceInfo: String,
) {

    val confirmationWindowSeconds: Long = CONFIRMATION_WINDOW.inWholeSeconds

    suspend fun start(): RequestResult<TelegramLoginStart> = authRepository.startTelegramLogin()

    suspend fun awaitSignIn(
        claimToken: String,
        onSecondLeft: suspend (Long) -> Unit,
    ): RequestResult<Unit> = awaitAnswer(onSecondLeft) {
        authRepository.signInWithProvider(
            provider = AuthProvider.TELEGRAM,
            token = claimToken,
            deviceInfo = deviceInfo,
        )
    }

    suspend fun awaitLink(
        claimToken: String,
        onSecondLeft: suspend (Long) -> Unit,
    ): RequestResult<Unit> = awaitAnswer(onSecondLeft) {
        val linked = identitiesRepository.linkProvider(provider = AuthProvider.TELEGRAM, token = claimToken)
        when (linked) {
            is RequestResult.Error -> linked
            is RequestResult.Success -> RequestResult.Success(Unit)
        }
    }

    private suspend fun awaitAnswer(
        onSecondLeft: suspend (Long) -> Unit,
        ask: suspend () -> RequestResult<Unit>,
    ): RequestResult<Unit> {
        val attempts = (CONFIRMATION_WINDOW / CONFIRMATION_POLL_DELAY).toInt()
        repeat(attempts) { attempt ->
            delay(CONFIRMATION_POLL_DELAY)
            onSecondLeft(CONFIRMATION_WINDOW.inWholeSeconds - CONFIRMATION_POLL_DELAY.inWholeSeconds * (attempt + 1))
            val answer = ask()
            val stillWaiting = answer is RequestResult.Error && answer.kind == RequestFailure.Conflict
            if (!stillWaiting) return answer
        }
        return RequestResult.Error(
            kind = RequestFailure.Gone,
            statusCode = null,
            userMessage = "",
            devMessage = "telegram confirmation window is over",
        )
    }
}
