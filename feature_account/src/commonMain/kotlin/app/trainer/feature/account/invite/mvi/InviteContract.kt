package app.trainer.feature.account.invite.mvi

import app.trainer.entities.RequestResult
import app.trainer.uikit.widgets.CODE_LENGTH

data class InviteState(
    val afterSessionExpiry: Boolean,
    val code: String,
    val isChecking: Boolean,
    val codeError: String?,
) {

    val isSubmitEnabled: Boolean
        get() = code.length == CODE_LENGTH && !isChecking

    companion object {

        fun initial(afterSessionExpiry: Boolean): InviteState = InviteState(
            afterSessionExpiry = afterSessionExpiry,
            code = "",
            isChecking = false,
            codeError = null,
        )
    }
}

sealed interface InviteEvent {

    data object OnSubmitClicked : InviteEvent

    data class OnCodeChanged(val code: String) : InviteEvent
}

sealed interface InviteSideEffect {

    data class OpenOnboarding(val code: String, val coachDisplayName: String) : InviteSideEffect

    data object SignedIn : InviteSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : InviteSideEffect
}
