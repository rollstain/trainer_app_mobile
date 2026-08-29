package app.trainer.feature.account.profile.mvi

import app.trainer.data.clients.CoachPolicy
import app.trainer.entities.RequestResult

data class ProfileState(
    val restSeconds: Int,
    val displayName: String,
    val roleLabel: String,
    val contactLabel: String?,
    val policy: CoachPolicy?,
    val isCoach: Boolean,
    val isOwner: Boolean,
    val isSignOutDialogVisible: Boolean,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasContact: Boolean
        get() = contactLabel != null

    companion object {

        fun initial(): ProfileState = ProfileState(
            restSeconds = 0,
            displayName = "",
            roleLabel = "",
            contactLabel = null,
            policy = null,
            isCoach = false,
            isOwner = false,
            isSignOutDialogVisible = false,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface ProfileEvent {

    data object OnReloadRequested : ProfileEvent

    data object OnAddContactClicked : ProfileEvent

    data object OnBecomeCoachClicked : ProfileEvent

    data object OnLoginMethodsClicked : ProfileEvent

    data object OnDevicesClicked : ProfileEvent

    data object OnExerciseLibraryClicked : ProfileEvent

    data object OnCoachesClicked : ProfileEvent

    data object OnProgramsClicked : ProfileEvent

    data class OnCancellationWindowSelected(val hours: Int) : ProfileEvent

    data class OnReminderHourSelected(val hour: Int) : ProfileEvent

    data class OnRestSecondsSelected(val seconds: Int) : ProfileEvent

    data class OnSessionRemindersToggled(val enabled: Boolean) : ProfileEvent

    data class OnDiaryRemindersToggled(val enabled: Boolean) : ProfileEvent

    data class OnCheckInRemindersToggled(val enabled: Boolean) : ProfileEvent

    data object OnSignOutClicked : ProfileEvent

    data object OnSignOutConfirmed : ProfileEvent

    data object OnSignOutDismissed : ProfileEvent
}

sealed interface ProfileSideEffect {

    data object OpenContactLink : ProfileSideEffect

    data object OpenCoachSetup : ProfileSideEffect

    data object OpenLoginMethods : ProfileSideEffect

    data object OpenDevices : ProfileSideEffect

    data object OpenExerciseLibrary : ProfileSideEffect

    data object OpenCoaches : ProfileSideEffect

    data object OpenPrograms : ProfileSideEffect

    data object SignedOut : ProfileSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ProfileSideEffect
}
