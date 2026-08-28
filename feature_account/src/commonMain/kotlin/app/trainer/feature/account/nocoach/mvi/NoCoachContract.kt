package app.trainer.feature.account.nocoach.mvi

import app.trainer.entities.RequestResult
import app.trainer.uikit.widgets.CODE_LENGTH

data class NoCoachState(
    val code: String,
    val isJoining: Boolean,
    val codeError: String?,
    val isSignOutDialogVisible: Boolean,
) {

    val isSubmitEnabled: Boolean
        get() = code.length == CODE_LENGTH && !isJoining

    companion object {

        fun initial(): NoCoachState = NoCoachState(
            code = "",
            isJoining = false,
            codeError = null,
            isSignOutDialogVisible = false,
        )
    }
}

sealed interface NoCoachEvent {

    data class OnCodeChanged(val code: String) : NoCoachEvent

    data object OnJoinClicked : NoCoachEvent

    data object OnSignOutClicked : NoCoachEvent

    data object OnSignOutConfirmed : NoCoachEvent

    data object OnSignOutDismissed : NoCoachEvent
}

sealed interface NoCoachSideEffect {

    data object Joined : NoCoachSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : NoCoachSideEffect
}
