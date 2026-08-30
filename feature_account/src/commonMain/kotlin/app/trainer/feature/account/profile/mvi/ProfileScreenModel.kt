package app.trainer.feature.account.profile.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.formatWorkingSchedule
import app.trainer.data.auth.AuthRepository
import app.trainer.data.clients.CoachPolicy
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.profile.UserProfile
import app.trainer.data.traininglog.RestIntervalStore
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.profile_client_role
import app.trainer.strings.profile_coach_role
import org.jetbrains.compose.resources.getString

class ProfileScreenModel(
    private val profileRepository: ProfileRepository,
    private val participantsRepository: ParticipantsRepository,
    private val authRepository: AuthRepository,
    private val restIntervalStore: RestIntervalStore,
) : BaseScreenModel<ProfileState, ProfileSideEffect, ProfileEvent>(
    initialState = ProfileState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = profileRepository.me()) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
                    postSideEffect(ProfileSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> showProfile(loaded.data)
            }
            val restSeconds = restIntervalStore.defaultSeconds()
            updateState { it.copy(restSeconds = restSeconds) }
        }
    }

    override fun dispatch(event: ProfileEvent) {
        when (event) {
            ProfileEvent.OnReloadRequested -> onFetchData()
            ProfileEvent.OnAddContactClicked -> openContactLink()
            ProfileEvent.OnProgramsClicked -> screenModelScope {
                postSideEffect(ProfileSideEffect.OpenPrograms)
            }
            ProfileEvent.OnBecomeCoachClicked -> screenModelScope {
                postSideEffect(ProfileSideEffect.OpenCoachSetup)
            }
            ProfileEvent.OnLoginMethodsClicked -> screenModelScope {
                postSideEffect(ProfileSideEffect.OpenLoginMethods)
            }
            ProfileEvent.OnDevicesClicked -> screenModelScope {
                postSideEffect(ProfileSideEffect.OpenDevices)
            }
            ProfileEvent.OnExerciseLibraryClicked -> screenModelScope {
                postSideEffect(ProfileSideEffect.OpenExerciseLibrary)
            }
            ProfileEvent.OnCoachesClicked -> screenModelScope {
                postSideEffect(ProfileSideEffect.OpenCoaches)
            }
            ProfileEvent.OnWorkingHoursClicked -> screenModelScope {
                postSideEffect(ProfileSideEffect.OpenWorkingHours)
            }
            is ProfileEvent.OnCancellationWindowSelected -> changePolicy { policy ->
                policy.copy(cancellationWindowHours = event.hours)
            }
            is ProfileEvent.OnReminderHourSelected -> changePolicy { policy ->
                policy.copy(reminderHour = event.hour)
            }
            is ProfileEvent.OnRestSecondsSelected -> screenModelScope {
                restIntervalStore.rememberDefault(event.seconds)
                updateState { it.copy(restSeconds = event.seconds) }
            }
            is ProfileEvent.OnSessionRemindersToggled -> changePolicy { policy ->
                policy.copy(sessionRemindersEnabled = event.enabled)
            }
            is ProfileEvent.OnDiaryRemindersToggled -> changePolicy { policy ->
                policy.copy(diaryRemindersEnabled = event.enabled)
            }
            is ProfileEvent.OnCheckInRemindersToggled -> changePolicy { policy ->
                policy.copy(checkInRemindersEnabled = event.enabled)
            }
            ProfileEvent.OnSignOutClicked -> updateState { it.copy(isSignOutDialogVisible = true) }
            ProfileEvent.OnSignOutDismissed -> updateState { it.copy(isSignOutDialogVisible = false) }
            ProfileEvent.OnSignOutConfirmed -> signOut()
        }
    }

    private suspend fun showProfile(profile: UserProfile) {
        val isCoach = profile.coachId != null
        val policy = if (isCoach) loadPolicy() else null
        val roleLabel = if (isCoach) {
            getString(Res.string.profile_coach_role)
        } else {
            getString(Res.string.profile_client_role)
        }
        val workingHoursLabel = workingHoursLabelOf(policy)
        updateState { current ->
            current.copy(
                displayName = profile.displayName,
                roleLabel = roleLabel,
                contactLabel = profile.phone ?: profile.email,
                policy = policy,
                workingHoursLabel = workingHoursLabel,
                isCoach = isCoach,
                isOwner = profile.isOwner,
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun workingHoursLabelOf(policy: CoachPolicy?): String? =
        policy?.workingHours?.takeIf { it.isNotEmpty() }?.let { formatWorkingSchedule(it) }

    private suspend fun loadPolicy(): CoachPolicy? {
        return when (val loaded = participantsRepository.coachPolicy()) {
            is RequestResult.Error -> {
                postSideEffect(ProfileSideEffect.ShowFailure(loaded))
                null
            }
            is RequestResult.Success -> loaded.data
        }
    }

    private fun changePolicy(change: (CoachPolicy) -> CoachPolicy) {
        screenModelScope { state ->
            val current = state.policy ?: return@screenModelScope
            val changed = change(current)
            if (changed == current) return@screenModelScope
            updateState { it.copy(policy = changed) }
            when (val saved = participantsRepository.updateCoachPolicy(policy = changed)) {
                is RequestResult.Error -> {
                    updateState { it.copy(policy = current) }
                    postSideEffect(ProfileSideEffect.ShowFailure(saved))
                }
                is RequestResult.Success -> updateState { it.copy(policy = saved.data) }
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
