package app.trainer.feature.account.nocoach.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.CoachAccessStatus
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.invite_code_expired_message
import app.trainer.strings.invite_code_not_found_message
import app.trainer.strings.invite_code_used_message
import app.trainer.uikit.widgets.CODE_LENGTH
import org.jetbrains.compose.resources.getString

class NoCoachScreenModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<NoCoachState, NoCoachSideEffect, NoCoachEvent>(
    initialState = NoCoachState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            val profile = profileRepository.me()
            if (profile is RequestResult.Success) {
                updateState { it.copy(displayName = profile.data.displayName) }
            }
            val status = authRepository.coachAccessStatus()
            if (status is RequestResult.Success) {
                updateState { it.copy(coachAccess = accessOf(status.data)) }
            }
        }
    }

    override fun dispatch(event: NoCoachEvent) {
        when (event) {
            is NoCoachEvent.OnCodeChanged -> changeCode(event.code)
            NoCoachEvent.OnJoinClicked -> join()
            NoCoachEvent.OnCoachAccessClicked -> askCoachAccess()
            NoCoachEvent.OnSignOutClicked -> updateState { it.copy(isSignOutDialogVisible = true) }
            NoCoachEvent.OnSignOutDismissed -> updateState { it.copy(isSignOutDialogVisible = false) }
            NoCoachEvent.OnSignOutConfirmed -> signOut()
        }
    }

    private fun changeCode(code: String) {
        val isPasted = code.length - state.code.length > 1
        updateState { it.copy(code = code, codeError = null) }
        if (isPasted && code.length == CODE_LENGTH) {
            join()
        }
    }

    private fun join() {
        screenModelScope { current ->
            if (!current.isSubmitEnabled) return@screenModelScope
            updateState { it.copy(isJoining = true, codeError = null) }
            val joined = authRepository.joinCoach(current.code)
            updateState { it.copy(isJoining = false) }
            when (joined) {
                is RequestResult.Error -> showCodeError(joined)
                is RequestResult.Success -> postSideEffect(NoCoachSideEffect.Joined)
            }
        }
    }

    private fun askCoachAccess() {
        screenModelScope { current ->
            if (current.coachAccess == CoachAccess.Asking) return@screenModelScope
            updateState { it.copy(coachAccess = CoachAccess.Asking) }
            when (val asked = authRepository.askCoachAccess()) {
                is RequestResult.Error -> {
                    updateState { it.copy(coachAccess = CoachAccess.NotAsked) }
                    postSideEffect(NoCoachSideEffect.ShowFailure(asked))
                }
                is RequestResult.Success -> updateState { it.copy(coachAccess = accessOf(asked.data)) }
            }
        }
    }

    private fun signOut() {
        screenModelScope {
            updateState { it.copy(isSignOutDialogVisible = false) }
            authRepository.logout()
        }
    }

    private fun accessOf(status: CoachAccessStatus): CoachAccess = when (status) {
        CoachAccessStatus.NONE -> CoachAccess.NotAsked
        CoachAccessStatus.PENDING -> CoachAccess.Pending
        CoachAccessStatus.DECLINED -> CoachAccess.Declined
        CoachAccessStatus.APPROVED -> CoachAccess.Pending
    }

    private suspend fun showCodeError(failure: RequestResult.Error) {
        val message = when (failure.kind) {
            RequestFailure.Gone -> getString(Res.string.invite_code_expired_message)
            RequestFailure.Conflict -> getString(Res.string.invite_code_used_message)
            RequestFailure.NotFound -> getString(Res.string.invite_code_not_found_message)
            else -> null
        }
        if (message == null) {
            postSideEffect(NoCoachSideEffect.ShowFailure(failure))
            return
        }
        val keepsTypedCode = failure.kind == RequestFailure.NotFound
        updateState { current ->
            current.copy(
                codeError = message,
                code = if (keepsTypedCode) current.code else "",
            )
        }
    }
}
