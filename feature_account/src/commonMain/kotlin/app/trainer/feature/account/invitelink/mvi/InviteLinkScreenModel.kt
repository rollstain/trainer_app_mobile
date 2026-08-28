package app.trainer.feature.account.invitelink.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult

class InviteLinkScreenModel(
    private val code: String,
    private val authRepository: AuthRepository,
    private val deviceInfo: String,
) : BaseScreenModel<InviteLinkState, InviteLinkSideEffect, InviteLinkEvent>(
    initialState = InviteLinkState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(content = InviteLinkContent.Loading) }
            val preview = authRepository.previewInvite(code)
            updateState {
                it.copy(
                    content = when (preview) {
                        is RequestResult.Error -> contentOf(preview)
                        is RequestResult.Success -> InviteLinkContent.Coach(
                            displayName = preview.data.coachDisplayName,
                            needsDisplayName = preview.data.needsDisplayName,
                        )
                    }
                )
            }
        }
    }

    override fun dispatch(event: InviteLinkEvent) {
        when (event) {
            InviteLinkEvent.OnReloadRequested -> onFetchData()
            InviteLinkEvent.OnCodeEntryClicked -> screenModelScope {
                postSideEffect(InviteLinkSideEffect.OpenCodeEntry)
            }
            InviteLinkEvent.OnJoinClicked -> join()
        }
    }

    private fun join() {
        screenModelScope { current ->
            val coach = current.content as? InviteLinkContent.Coach ?: return@screenModelScope
            if (current.isJoining) return@screenModelScope
            if (coach.needsDisplayName) {
                postSideEffect(InviteLinkSideEffect.OpenOnboarding(code = code))
                return@screenModelScope
            }
            updateState { it.copy(isJoining = true) }
            val joined = authRepository.redeemInvite(code = code, displayName = "", deviceInfo = deviceInfo)
            updateState { it.copy(isJoining = false) }
            when (joined) {
                is RequestResult.Error -> postSideEffect(InviteLinkSideEffect.ShowFailure(joined))
                is RequestResult.Success -> postSideEffect(InviteLinkSideEffect.SignedIn)
            }
        }
    }

    private fun contentOf(failure: RequestResult.Error): InviteLinkContent = when (failure.kind) {
        RequestFailure.Gone -> InviteLinkContent.Problem(InviteLinkProblem.Expired)
        RequestFailure.Conflict -> InviteLinkContent.Problem(InviteLinkProblem.AlreadyUsed)
        RequestFailure.NotFound -> InviteLinkContent.Problem(InviteLinkProblem.NotFound)
        else -> InviteLinkContent.Failure(failure)
    }
}
