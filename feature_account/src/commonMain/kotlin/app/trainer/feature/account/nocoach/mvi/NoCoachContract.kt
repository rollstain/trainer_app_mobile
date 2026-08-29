package app.trainer.feature.account.nocoach.mvi

import app.trainer.entities.RequestResult
import app.trainer.uikit.widgets.CODE_LENGTH

sealed interface CoachAccessState {

    data object NotAsked : CoachAccessState

    data class Pending(val askedAtLabel: String) : CoachAccessState

    data class Declined(
        val decidedAtLabel: String,
        val canAskAgainLabel: String,
        val canAskAgain: Boolean,
    ) : CoachAccessState
}

data class NoCoachState(
    val displayName: String,
    val code: String,
    val access: CoachAccessState,
    val isJoining: Boolean,
    val codeError: String?,
    val isSignOutDialogVisible: Boolean,
) {

    val isSubmitEnabled: Boolean
        get() = code.length == CODE_LENGTH && !isJoining

    val isWaitingDecision: Boolean
        get() = access is CoachAccessState.Pending

    companion object {

        fun initial(): NoCoachState = NoCoachState(
            displayName = "",
            code = "",
            access = CoachAccessState.NotAsked,
            isJoining = false,
            codeError = null,
            isSignOutDialogVisible = false,
        )
    }
}

sealed interface NoCoachEvent {

    data object OnReloadRequested : NoCoachEvent

    data class OnCodeChanged(val code: String) : NoCoachEvent

    data object OnJoinClicked : NoCoachEvent

    data object OnApplicationClicked : NoCoachEvent

    data object OnSignOutClicked : NoCoachEvent

    data object OnSignOutConfirmed : NoCoachEvent

    data object OnSignOutDismissed : NoCoachEvent
}

sealed interface NoCoachSideEffect {

    data object Joined : NoCoachSideEffect

    data object OpenApplication : NoCoachSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : NoCoachSideEffect
}
