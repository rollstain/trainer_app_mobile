package app.trainer.feature.progress.presentation.formcheck.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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
    val nextCursor: String?,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasMore: Boolean
        get() = nextCursor != null

    fun withFirstPage(rows: List<AwaitingFormCheck>, nextCursor: String?): CoachFormChecksState = copy(
        checks = rows.toImmutableList(),
        nextCursor = nextCursor,
        isLoading = false,
        isLoadingMore = false,
        failure = null,
    )

    fun withNextPage(rows: List<AwaitingFormCheck>, nextCursor: String?): CoachFormChecksState {
        val known = checks.mapTo(mutableSetOf(), AwaitingFormCheck::formCheckId)
        return copy(
            checks = (checks + rows.filterNot { it.formCheckId in known }).toImmutableList(),
            nextCursor = nextCursor,
            isLoadingMore = false,
        )
    }

    companion object {

        fun initial(): CoachFormChecksState = CoachFormChecksState(
            checks = persistentListOf(),
            nextCursor = null,
            isLoading = true,
            isLoadingMore = false,
            failure = null,
        )
    }
}

sealed interface CoachFormChecksEvent {

    data object OnReloadRequested : CoachFormChecksEvent

    data object OnEndReached : CoachFormChecksEvent

    data class OnDraftChanged(val formCheckId: String, val text: String) : CoachFormChecksEvent

    data class OnReplyClicked(val formCheckId: String) : CoachFormChecksEvent
}

sealed interface CoachFormChecksSideEffect {

    data object ShowReplied : CoachFormChecksSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : CoachFormChecksSideEffect
}
