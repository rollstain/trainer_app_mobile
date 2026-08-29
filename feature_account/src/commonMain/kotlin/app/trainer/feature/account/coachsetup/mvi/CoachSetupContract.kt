package app.trainer.feature.account.coachsetup.mvi

import app.trainer.entities.RequestResult

data class CoachSetupState(
    val displayName: String,
    val zoneId: String,
    val isSending: Boolean,
) {

    val isStartEnabled: Boolean
        get() = displayName.isNotBlank() && !isSending

    companion object {

        fun initial(zoneId: String): CoachSetupState = CoachSetupState(
            displayName = "",
            zoneId = zoneId,
            isSending = false,
        )
    }
}

sealed interface CoachSetupEvent {

    data class OnDisplayNameChanged(val displayName: String) : CoachSetupEvent

    data object OnStartClicked : CoachSetupEvent
}

sealed interface CoachSetupSideEffect {

    data object Started : CoachSetupSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : CoachSetupSideEffect
}
