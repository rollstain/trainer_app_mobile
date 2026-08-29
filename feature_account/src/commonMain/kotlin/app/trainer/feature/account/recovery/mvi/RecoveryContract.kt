package app.trainer.feature.account.recovery.mvi

import app.trainer.entities.RequestResult
import app.trainer.feature.account.telegram.TelegramLoginState

sealed interface RecoveryStep {

    data object Asking : RecoveryStep

    data class LetterSent(val resendSecondsLeft: Long) : RecoveryStep

    data object LetterRefused : RecoveryStep

    data object NothingLeft : RecoveryStep
}

data class RecoveryState(
    val email: String,
    val isSending: Boolean,
    val step: RecoveryStep,
    val telegram: TelegramLoginState,
    val claimToken: String?,
) {

    val isSendEnabled: Boolean
        get() = email.contains(EMAIL_MARKER) && !isSending && resendSecondsLeft() == NO_WAIT

    fun resendSecondsLeft(): Long = (step as? RecoveryStep.LetterSent)?.resendSecondsLeft ?: NO_WAIT

    companion object {

        private const val EMAIL_MARKER = '@'
        const val NO_WAIT = 0L

        fun initial(email: String): RecoveryState = RecoveryState(
            email = email,
            isSending = false,
            step = RecoveryStep.Asking,
            telegram = TelegramLoginState.Idle,
            claimToken = null,
        )
    }
}

sealed interface RecoveryEvent {

    data class OnEmailChanged(val value: String) : RecoveryEvent

    data object OnSendClicked : RecoveryEvent

    data object OnTelegramClicked : RecoveryEvent

    data object OnTelegramCancelled : RecoveryEvent

    data object OnTelegramConfirmed : RecoveryEvent

    data object OnCodeClicked : RecoveryEvent
}

sealed interface RecoverySideEffect {

    data class OpenTelegram(val deepLink: String) : RecoverySideEffect

    data class OpenNewPassword(val claimToken: String) : RecoverySideEffect

    data object OpenCodeEntry : RecoverySideEffect

    data class ShowFailure(val failure: RequestResult.Error) : RecoverySideEffect
}
