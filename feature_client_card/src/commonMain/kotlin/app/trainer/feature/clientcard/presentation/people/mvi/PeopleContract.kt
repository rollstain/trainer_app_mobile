package app.trainer.feature.clientcard.presentation.people.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PersonRow(
    val userId: String,
    val displayName: String,
    val hasMedicalNotes: Boolean,
    val nextSessionLabel: String?,
    val hasPendingChangeRequest: Boolean,
    val unreadCount: Long,
)

data class PeopleState(
    val people: ImmutableList<PersonRow>,
    val isLoading: Boolean,
    val isCreatingInvite: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(): PeopleState = PeopleState(
            people = persistentListOf(),
            isLoading = true,
            isCreatingInvite = false,
            failure = null,
        )
    }
}

sealed interface PeopleEvent {

    data object OnRetryClicked : PeopleEvent

    data object OnCreateInviteClicked : PeopleEvent

    data class OnPersonClicked(val userId: String) : PeopleEvent
}

sealed interface PeopleSideEffect {

    data class OpenPerson(val userId: String) : PeopleSideEffect

    data class ShowInviteCode(val code: String) : PeopleSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : PeopleSideEffect
}
