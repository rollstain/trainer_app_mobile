package app.trainer.feature.account.coachrequests.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CoachRequestRow(
    val requestId: String,
    val displayName: String,
    val askedAtLabel: String,
)

data class CoachRequestsState(
    val requests: ImmutableList<CoachRequestRow>,
    val decidingId: String?,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(): CoachRequestsState = CoachRequestsState(
            requests = persistentListOf(),
            decidingId = null,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface CoachRequestsEvent {

    data object OnReloadRequested : CoachRequestsEvent

    data class OnApproved(val requestId: String) : CoachRequestsEvent

    data class OnDeclined(val requestId: String) : CoachRequestsEvent
}

sealed interface CoachRequestsSideEffect {

    data object ShowApproved : CoachRequestsSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : CoachRequestsSideEffect
}
