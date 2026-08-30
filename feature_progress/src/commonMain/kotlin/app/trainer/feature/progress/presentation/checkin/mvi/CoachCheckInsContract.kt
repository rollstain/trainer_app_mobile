package app.trainer.feature.progress.presentation.checkin.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class AwaitingCheckInRow(
    val checkInId: String,
    val clientUserId: String,
    val clientDisplayName: String,
    val dateLabel: String,
)

data class CoachCheckInsState(
    val checkIns: ImmutableList<AwaitingCheckInRow>,
    val nextCursor: String?,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasMore: Boolean
        get() = nextCursor != null

    fun withFirstPage(rows: List<AwaitingCheckInRow>, nextCursor: String?): CoachCheckInsState = copy(
        checkIns = rows.toImmutableList(),
        nextCursor = nextCursor,
        isLoading = false,
        isLoadingMore = false,
        failure = null,
    )

    fun withNextPage(rows: List<AwaitingCheckInRow>, nextCursor: String?): CoachCheckInsState {
        val known = checkIns.mapTo(mutableSetOf(), AwaitingCheckInRow::checkInId)
        return copy(
            checkIns = (checkIns + rows.filterNot { it.checkInId in known }).toImmutableList(),
            nextCursor = nextCursor,
            isLoadingMore = false,
        )
    }

    companion object {

        fun initial(): CoachCheckInsState = CoachCheckInsState(
            checkIns = persistentListOf(),
            nextCursor = null,
            isLoading = true,
            isLoadingMore = false,
            failure = null,
        )
    }
}

sealed interface CoachCheckInsEvent {

    data object OnReloadRequested : CoachCheckInsEvent

    data object OnEndReached : CoachCheckInsEvent

    data class OnCheckInClicked(val clientUserId: String) : CoachCheckInsEvent
}

sealed interface CoachCheckInsSideEffect {

    data class OpenClientCard(val clientUserId: String) : CoachCheckInsSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : CoachCheckInsSideEffect
}
