package app.trainer.feature.progress.presentation.formcheck.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AwaitingFormCheck(
    val formCheckId: String,
    val clientDisplayName: String,
    val dateLabel: String,
    val exerciseName: String?,
    val note: String?,
    val videoUrl: String?,
    val draft: String,
    val isSending: Boolean,
)

data class CoachFormChecksState(
    val checks: ImmutableList<AwaitingFormCheck>,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(): CoachFormChecksState = CoachFormChecksState(
            checks = persistentListOf(),
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface CoachFormChecksEvent {

    data object OnReloadRequested : CoachFormChecksEvent

    data class OnDraftChanged(val formCheckId: String, val text: String) : CoachFormChecksEvent

    data class OnReplyClicked(val formCheckId: String) : CoachFormChecksEvent
}

sealed interface CoachFormChecksSideEffect {

    data object ShowReplied : CoachFormChecksSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : CoachFormChecksSideEffect
}
