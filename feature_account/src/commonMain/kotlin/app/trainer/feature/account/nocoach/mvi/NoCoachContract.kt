package app.trainer.feature.account.nocoach.mvi

import app.trainer.entities.RequestResult
import app.trainer.uikit.widgets.CODE_LENGTH

sealed interface CoachAccess {

    data object NotAsked : CoachAccess

    data object Asking : CoachAccess

    data object Pending : CoachAccess

    data object Declined : CoachAccess
}

data class NoCoachState(
    val displayName: String,
    val code: String,
    val coachAccess: CoachAccess,
    val isJoining: Boolean,
    val codeError: String?,
    val isSignOutDialogVisible: Boolean,
) {

    val isSubmitEnabled: Boolean
        get() = code.length == CODE_LENGTH && !isJoining

    companion object {

        fun initial(): NoCoachState = NoCoachState(
            displayName = "",
            code = "",
            coachAccess = CoachAccess.NotAsked,
            isJoining = false,
            codeError = null,
            isSignOutDialogVisible = false,
        )
    }
}

sealed interface NoCoachEvent {

    data class OnCodeChanged(val code: String) : NoCoachEvent

    data object OnJoinClicked : NoCoachEvent

    data object OnCoachAccessClicked : NoCoachEvent

    data object OnSignOutClicked : NoCoachEvent

    data object OnSignOutConfirmed : NoCoachEvent

    data object OnSignOutDismissed : NoCoachEvent
}

sealed interface NoCoachSideEffect {

    data object Joined : NoCoachSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : NoCoachSideEffect
}
