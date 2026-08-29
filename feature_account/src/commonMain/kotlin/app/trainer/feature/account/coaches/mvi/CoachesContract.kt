package app.trainer.feature.account.coaches.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class CoachRow(
    val coachId: String,
    val displayName: String,
    val joinedLabel: String,
    val activeClients: Int,
    val isOwner: Boolean,
)

data class CoachesState(
    val coaches: ImmutableList<CoachRow>,
    val nextCursor: String?,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasMore: Boolean
        get() = nextCursor != null

    fun withFirstPage(rows: List<CoachRow>, nextCursor: String?): CoachesState = copy(
        coaches = rows.toImmutableList(),
        nextCursor = nextCursor,
        isLoading = false,
        isLoadingMore = false,
        failure = null,
    )

    fun withNextPage(rows: List<CoachRow>, nextCursor: String?): CoachesState {
        val known = coaches.mapTo(mutableSetOf(), CoachRow::coachId)
        return copy(
            coaches = (coaches + rows.filterNot { it.coachId in known }).toImmutableList(),
            nextCursor = nextCursor,
            isLoadingMore = false,
        )
    }

    companion object {

        fun initial(): CoachesState = CoachesState(
            coaches = persistentListOf(),
            nextCursor = null,
            isLoading = true,
            isLoadingMore = false,
            failure = null,
        )
    }
}

sealed interface CoachesEvent {

    data object OnReloadRequested : CoachesEvent

    data object OnEndReached : CoachesEvent

    data class OnCoachClicked(val coachId: String) : CoachesEvent
}

sealed interface CoachesSideEffect {

    data class OpenCoach(val coachId: String) : CoachesSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : CoachesSideEffect
}
