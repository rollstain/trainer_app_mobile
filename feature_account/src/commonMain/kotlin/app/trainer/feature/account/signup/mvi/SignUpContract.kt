package app.trainer.feature.account.signup.mvi

import app.trainer.entities.RequestResult

const val PASSWORD_MIN_LENGTH = 8

sealed interface LoginField {

    data object Hidden : LoginField

    data class Shown(val value: String, val error: String?) : LoginField
}

data class SignUpState(
    val name: String,
    val email: String,
    val emailError: String?,
    val login: LoginField,
    val password: String,
    val isPasswordRevealed: Boolean,
    val isSubmitting: Boolean,
) {

    val charsMissing: Int
        get() = (PASSWORD_MIN_LENGTH - password.length).coerceAtLeast(0)

    val isSubmitEnabled: Boolean
        get() = name.isNotBlank() && email.contains(EMAIL_MARKER) && charsMissing == 0 && !isSubmitting

    companion object {

        private const val EMAIL_MARKER = '@'

        fun initial(): SignUpState = SignUpState(
            name = "",
            email = "",
            emailError = null,
            login = LoginField.Hidden,
            password = "",
            isPasswordRevealed = true,
            isSubmitting = false,
        )
    }
}

sealed interface SignUpEvent {

    data class OnNameChanged(val value: String) : SignUpEvent

    data class OnEmailChanged(val value: String) : SignUpEvent

    data object OnLoginRequested : SignUpEvent

    data class OnLoginChanged(val value: String) : SignUpEvent

    data object OnLoginCleared : SignUpEvent

    data class OnPasswordChanged(val value: String) : SignUpEvent

    data object OnRevealToggled : SignUpEvent

    data object OnSubmitClicked : SignUpEvent
}

sealed interface SignUpSideEffect {

    data object SignedUp : SignUpSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : SignUpSideEffect
}
