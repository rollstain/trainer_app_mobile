package app.trainer.feature.account.profile.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.profile.UserProfile
import app.trainer.entities.RequestResult

private const val COACH_ROLE = "тренер"
private const val CLIENT_ROLE = "подопечный"

class ProfileScreenModel(
    private val profileRepository: ProfileRepository,
    private val participantsRepository: ParticipantsRepository,
    private val authRepository: AuthRepository,
) : BaseScreenModel<ProfileState, ProfileSideEffect, ProfileEvent>(
    initialState = ProfileState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, isFailed = false) }
            when (val loaded = profileRepository.me()) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, isFailed = true) }
                    postSideEffect(ProfileSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> showProfile(loaded.data)
            }
        }
    }

    override fun dispatch(event: ProfileEvent) {
        when (event) {
            ProfileEvent.OnRetryClicked -> onFetchData()
            ProfileEvent.OnAddContactClicked -> openContactLink()
            is ProfileEvent.OnCancellationWindowSelected -> updateCancellationWindow(event.hours)
            ProfileEvent.OnSignOutClicked -> updateState { it.copy(isSignOutDialogVisible = true) }
            ProfileEvent.OnSignOutDismissed -> updateState { it.copy(isSignOutDialogVisible = false) }
            ProfileEvent.OnSignOutConfirmed -> signOut()
        }
    }

    private suspend fun showProfile(profile: UserProfile) {
        val isCoach = profile.coachId != null
        val cancellationWindowHours = if (isCoach) loadCancellationWindow() else null
        updateState { current ->
            current.copy(
                displayName = profile.displayName,
                roleLabel = if (isCoach) COACH_ROLE else CLIENT_ROLE,
                contactLabel = profile.phone ?: profile.email,
                cancellationWindowHours = cancellationWindowHours,
                isLoading = false,
                isFailed = false,
            )
        }
    }

    private suspend fun loadCancellationWindow(): Int? {
        return when (val policy = participantsRepository.coachPolicy()) {
            is RequestResult.Error -> {
                postSideEffect(ProfileSideEffect.ShowFailure(policy))
                null
            }
            is RequestResult.Success -> policy.data
        }
    }

    private fun updateCancellationWindow(hours: Int) {
        screenModelScope { state ->
            if (state.cancellationWindowHours == hours) return@screenModelScope
            when (val updated = participantsRepository.updateCoachPolicy(cancellationWindowHours = hours)) {
                is RequestResult.Error -> postSideEffect(ProfileSideEffect.ShowFailure(updated))
                is RequestResult.Success -> updateState {
                    it.copy(cancellationWindowHours = updated.data)
                }
            }
        }
    }

    private fun openContactLink() {
        screenModelScope {
            postSideEffect(ProfileSideEffect.OpenContactLink)
        }
    }

    private fun signOut() {
        screenModelScope {
            updateState { it.copy(isSignOutDialogVisible = false) }
            authRepository.logout()
            postSideEffect(ProfileSideEffect.SignedOut)
        }
    }
}
