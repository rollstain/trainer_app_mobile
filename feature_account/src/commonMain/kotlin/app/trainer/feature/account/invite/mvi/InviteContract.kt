package app.trainer.feature.account.invite.mvi

import app.trainer.entities.RequestResult

private const val INVITE_CODE_LENGTH = 6

data class InviteState(
    val code: String,
    val displayName: String,
    val isSubmitting: Boolean,
    val codeError: String?,
) {

    val isSubmitEnabled: Boolean
        get() = code.length == INVITE_CODE_LENGTH && displayName.isNotBlank() && !isSubmitting

    companion object {

        fun initial(prefilledCode: String?): InviteState = InviteState(
            code = prefilledCode.orEmpty(),
            displayName = "",
            isSubmitting = false,
            codeError = null,
        )
    }
}

sealed interface InviteEvent {

    data object OnSubmitClicked : InviteEvent

    data class OnCodeChanged(val code: String) : InviteEvent

    data class OnDisplayNameChanged(val displayName: String) : InviteEvent
}

sealed interface InviteSideEffect {

    data object OpenContactLink : InviteSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : InviteSideEffect
}
