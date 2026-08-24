package app.trainer.feature.account.profile.mvi

import app.trainer.entities.RequestResult

data class ProfileState(
    val displayName: String,
    val roleLabel: String,
    val contactLabel: String?,
    val cancellationWindowHours: Int?,
    val isSignOutDialogVisible: Boolean,
    val isLoading: Boolean,
    val isFailed: Boolean,
) {

    val hasContact: Boolean
        get() = contactLabel != null

    companion object {

        fun initial(): ProfileState = ProfileState(
            displayName = "",
            roleLabel = "",
            contactLabel = null,
            cancellationWindowHours = null,
            isSignOutDialogVisible = false,
            isLoading = true,
            isFailed = false,
        )
    }
}

sealed interface ProfileEvent {

    data object OnRetryClicked : ProfileEvent

    data object OnAddContactClicked : ProfileEvent

    data class OnCancellationWindowSelected(val hours: Int) : ProfileEvent

    data object OnSignOutClicked : ProfileEvent

    data object OnSignOutConfirmed : ProfileEvent

    data object OnSignOutDismissed : ProfileEvent
}

sealed interface ProfileSideEffect {

    data object OpenContactLink : ProfileSideEffect

    data object SignedOut : ProfileSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ProfileSideEffect
}
