package app.trainer.feature.account.invitelink.mvi

import app.trainer.entities.RequestResult

enum class InviteLinkProblem { Expired, AlreadyUsed, NotFound }

sealed interface InviteLinkContent {

    data object Loading : InviteLinkContent

    data class Coach(val displayName: String, val needsDisplayName: Boolean) : InviteLinkContent

    data class Problem(val kind: InviteLinkProblem) : InviteLinkContent

    data class Failure(val failure: RequestResult.Error) : InviteLinkContent
}

data class InviteLinkState(
    val content: InviteLinkContent,
    val isJoining: Boolean,
) {

    companion object {

        fun initial(): InviteLinkState = InviteLinkState(
            content = InviteLinkContent.Loading,
            isJoining = false,
        )
    }
}

sealed interface InviteLinkEvent {

    data object OnJoinClicked : InviteLinkEvent

    data object OnCodeEntryClicked : InviteLinkEvent

    data object OnReloadRequested : InviteLinkEvent
}

sealed interface InviteLinkSideEffect {

    data class OpenOnboarding(val code: String) : InviteLinkSideEffect

    data object OpenCodeEntry : InviteLinkSideEffect

    data object SignedIn : InviteLinkSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : InviteLinkSideEffect
}
