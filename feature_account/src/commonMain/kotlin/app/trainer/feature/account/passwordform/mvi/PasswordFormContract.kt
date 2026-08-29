package app.trainer.feature.account.passwordform.mvi

import app.trainer.entities.RequestResult

const val PASSWORD_MIN_LENGTH = 8

data class PasswordFormState(
    val hasPassword: Boolean,
    val needsEmail: Boolean,
    val email: String,
    val emailError: String?,
    val currentPassword: String,
    val currentPasswordError: String?,
    val newPassword: String,
    val isRevealed: Boolean,
    val isLoading: Boolean,
    val isSaving: Boolean,
) {

    val charsMissing: Int
        get() = (PASSWORD_MIN_LENGTH - newPassword.length).coerceAtLeast(0)

    val isSaveEnabled: Boolean
        get() = charsMissing == 0 && !isSaving &&
            (!hasPassword || currentPassword.isNotEmpty()) &&
            (!needsEmail || email.contains(EMAIL_MARKER))

    companion object {

        private const val EMAIL_MARKER = '@'

        fun initial(): PasswordFormState = PasswordFormState(
            hasPassword = false,
            needsEmail = false,
            email = "",
            emailError = null,
            currentPassword = "",
            currentPasswordError = null,
            newPassword = "",
            isRevealed = true,
            isLoading = true,
            isSaving = false,
        )
    }
}

sealed interface PasswordFormEvent {

    data class OnEmailChanged(val value: String) : PasswordFormEvent

    data class OnCurrentPasswordChanged(val value: String) : PasswordFormEvent

    data class OnNewPasswordChanged(val value: String) : PasswordFormEvent

    data object OnRevealToggled : PasswordFormEvent

    data object OnSaveClicked : PasswordFormEvent

    data object OnRecoveryClicked : PasswordFormEvent
}

sealed interface PasswordFormSideEffect {

    data object Saved : PasswordFormSideEffect

    data class OpenRecovery(val email: String) : PasswordFormSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : PasswordFormSideEffect
}
