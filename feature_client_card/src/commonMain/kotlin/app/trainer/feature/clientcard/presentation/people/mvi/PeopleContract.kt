package app.trainer.feature.clientcard.presentation.people.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val SEARCH_SHOWN_FROM_CLIENTS = 8

data class PersonRow(
    val userId: String,
    val displayName: String,
    val hasMedicalNotes: Boolean,
    val nextSessionLabel: String?,
    val hasPendingChangeRequest: Boolean,
    val unreadCount: Long,
    val attentionReason: String?,
)

data class PeopleState(
    val search: String,
    val isSearchable: Boolean,
    val booked: ImmutableList<PersonRow>,
    val others: ImmutableList<PersonRow>,
    val nextCursor: String?,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val isCreatingInvite: Boolean,
    val failure: RequestResult.Error?,
) {

    val isEmpty: Boolean
        get() = booked.isEmpty() && others.isEmpty()

    val isSearching: Boolean
        get() = search.isNotBlank()

    val hasMore: Boolean
        get() = nextCursor != null

    fun withFirstPage(
        booked: List<PersonRow>,
        others: List<PersonRow>,
        nextCursor: String?,
    ): PeopleState = copy(
        booked = booked.toImmutableList(),
        others = others.toImmutableList(),
        isSearchable = isSearchable || reachesSearchThreshold(
            loaded = booked.size + others.size,
            nextCursor = nextCursor,
        ),
        nextCursor = nextCursor,
        isLoading = false,
        isLoadingMore = false,
        failure = null,
    )

    fun withNextPage(rows: List<PersonRow>, nextCursor: String?): PeopleState {
        val known = (booked + others).mapTo(mutableSetOf(), PersonRow::userId)
        val fresh = rows.filterNot { it.userId in known }
        return copy(
            others = (others + fresh).toImmutableList(),
            nextCursor = nextCursor,
            isLoadingMore = false,
        )
    }

    private fun reachesSearchThreshold(loaded: Int, nextCursor: String?): Boolean =
        !isSearching && (loaded >= SEARCH_SHOWN_FROM_CLIENTS || nextCursor != null)

    companion object {

        fun initial(): PeopleState = PeopleState(
            search = "",
            isSearchable = false,
            booked = persistentListOf(),
            others = persistentListOf(),
            nextCursor = null,
            isLoading = true,
            isLoadingMore = false,
            isCreatingInvite = false,
            failure = null,
        )
    }
}

sealed interface PeopleEvent {

    data object OnRetryClicked : PeopleEvent

    data object OnEndReached : PeopleEvent

    data object OnCreateInviteClicked : PeopleEvent

    data class OnSearchChanged(val query: String) : PeopleEvent

    data class OnPersonClicked(val userId: String) : PeopleEvent
}

sealed interface PeopleSideEffect {

    data class OpenPerson(val userId: String) : PeopleSideEffect

    data class ShowInviteCode(val code: String) : PeopleSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : PeopleSideEffect
}
