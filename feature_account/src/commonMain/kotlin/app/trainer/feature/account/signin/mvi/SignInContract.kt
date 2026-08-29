package app.trainer.feature.account.signin.mvi

import app.trainer.entities.RequestResult
import app.trainer.feature.account.telegram.TelegramLoginState

sealed interface SignInFailure {

    data object None : SignInFailure

    data object Rejected : SignInFailure

    data object Offline : SignInFailure

    data class Locked(val secondsLeft: Long) : SignInFailure
}

data class SignInState(
    val identifier: String,
    val password: String,
    val isPasswordRevealed: Boolean,
    val isSubmitting: Boolean,
    val failure: SignInFailure,
    val telegram: TelegramLoginState,
) {

    val isSubmitEnabled: Boolean
        get() = identifier.isNotBlank() && password.isNotEmpty() && !isSubmitting && failure !is SignInFailure.Locked

    val isLocked: Boolean
        get() = failure is SignInFailure.Locked

    companion object {

        fun initial(): SignInState = SignInState(
            identifier = "",
            password = "",
            isPasswordRevealed = false,
            isSubmitting = false,
            failure = SignInFailure.None,
            telegram = TelegramLoginState.Idle,
        )
    }
}

sealed interface SignInEvent {

    data class OnIdentifierChanged(val value: String) : SignInEvent

    data class OnPasswordChanged(val value: String) : SignInEvent

    data object OnRevealToggled : SignInEvent

    data object OnSubmitClicked : SignInEvent

    data object OnForgotClicked : SignInEvent

    data object OnTelegramClicked : SignInEvent

    data object OnTelegramCancelled : SignInEvent
}

sealed interface SignInSideEffect {

    data object SignedIn : SignInSideEffect

    data class OpenRecovery(val email: String) : SignInSideEffect

    data class OpenTelegram(val deepLink: String) : SignInSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : SignInSideEffect
}
