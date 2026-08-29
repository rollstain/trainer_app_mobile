package app.trainer.feature.account.contact.mvi

import app.trainer.entities.RequestResult

enum class ContactKind { Phone, Email }

data class ContactLinkState(
    val kind: ContactKind,
    val value: String,
    val isSubmitting: Boolean,
    val needsCurrentPassword: Boolean,
    val currentPassword: String,
    val currentPasswordError: String?,
) {

    val isSubmitEnabled: Boolean
        get() = value.isNotBlank() && !isSubmitting &&
            (!needsCurrentPassword || currentPassword.isNotEmpty())

    companion object {

        fun initial(): ContactLinkState = ContactLinkState(
            kind = ContactKind.Phone,
            value = "",
            isSubmitting = false,
            needsCurrentPassword = false,
            currentPassword = "",
            currentPasswordError = null,
        )
    }
}

sealed interface ContactLinkEvent {

    data object OnSubmitClicked : ContactLinkEvent

    data object OnSkipClicked : ContactLinkEvent

    data class OnKindChanged(val kind: ContactKind) : ContactLinkEvent

    data class OnValueChanged(val value: String) : ContactLinkEvent

    data class OnCurrentPasswordChanged(val value: String) : ContactLinkEvent
}

sealed interface ContactLinkSideEffect {

    data object Dismissed : ContactLinkSideEffect

    data object Saved : ContactLinkSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ContactLinkSideEffect
}
