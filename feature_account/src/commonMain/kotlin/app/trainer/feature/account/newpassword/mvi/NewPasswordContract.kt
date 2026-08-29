package app.trainer.feature.account.newpassword.mvi

import app.trainer.entities.RequestResult

const val PASSWORD_MIN_LENGTH = 8

sealed interface LinkState {

    data object Usable : LinkState

    data object AlreadyUsed : LinkState

    data object Expired : LinkState

    data object NotConfirmedYet : LinkState
}

data class NewPasswordState(
    val password: String,
    val isRevealed: Boolean,
    val isSubmitting: Boolean,
    val link: LinkState,
) {

    val charsMissing: Int
        get() = (PASSWORD_MIN_LENGTH - password.length).coerceAtLeast(0)

    val isSubmitEnabled: Boolean
        get() = charsMissing == 0 && !isSubmitting && link == LinkState.Usable

    companion object {

        fun initial(): NewPasswordState = NewPasswordState(
            password = "",
            isRevealed = true,
            isSubmitting = false,
            link = LinkState.Usable,
        )
    }
}

sealed interface NewPasswordEvent {

    data class OnPasswordChanged(val value: String) : NewPasswordEvent

    data object OnRevealToggled : NewPasswordEvent

    data object OnSubmitClicked : NewPasswordEvent

    data object OnRequestNewLinkClicked : NewPasswordEvent
}

sealed interface NewPasswordSideEffect {

    data object PasswordChanged : NewPasswordSideEffect

    data object OpenRecovery : NewPasswordSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : NewPasswordSideEffect
}
