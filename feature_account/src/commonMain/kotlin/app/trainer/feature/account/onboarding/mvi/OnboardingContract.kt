package app.trainer.feature.account.onboarding.mvi

import app.trainer.entities.RequestResult

data class OnboardingState(
    val displayName: String,
    val isSaving: Boolean,
    val nameError: String?,
) {

    companion object {

        fun initial(): OnboardingState = OnboardingState(
            displayName = "",
            isSaving = false,
            nameError = null,
        )
    }
}

sealed interface OnboardingEvent {

    data class OnDisplayNameChanged(val displayName: String) : OnboardingEvent

    data object OnContinueClicked : OnboardingEvent
}

sealed interface OnboardingSideEffect {

    data object SignedIn : OnboardingSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : OnboardingSideEffect
}
