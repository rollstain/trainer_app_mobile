package app.trainer.feature.schedule.presentation.groupsession.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class GroupParticipantRow(
    val clientUserId: String,
    val displayName: String,
    val bookedAtLabel: String,
    val hasMedicalNotes: Boolean,
)

data class GroupWaitingRow(
    val clientUserId: String,
    val displayName: String,
    val joinedAtLabel: String,
)

data class GroupPickRow(
    val clientUserId: String,
    val displayName: String,
)

data class GroupSessionState(
    val title: String,
    val whenLabel: String,
    val takenSeats: Int,
    val freeSeats: Int,
    val participants: ImmutableList<GroupParticipantRow>,
    val waiting: ImmutableList<GroupWaitingRow>,
    val picker: ImmutableList<GroupPickRow>?,
    val isPickerLoading: Boolean,
    val isCompleted: Boolean,
    val isCancelled: Boolean,
    val isResolving: Boolean,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val isEmptySession: Boolean get() = participants.isEmpty()

    val hasFreeSeats: Boolean get() = freeSeats > 0

    val isOpenForChanges: Boolean
        get() = !isLoading && failure == null && !isCancelled && !isCompleted

    companion object {

        fun initial(): GroupSessionState = GroupSessionState(
            title = "",
            whenLabel = "",
            takenSeats = 0,
            freeSeats = 0,
            participants = persistentListOf(),
            waiting = persistentListOf(),
            picker = null,
            isPickerLoading = false,
            isCompleted = false,
            isCancelled = false,
            isResolving = false,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface GroupSessionEvent {

    data object OnReloadRequested : GroupSessionEvent

    data object OnAddParticipantClicked : GroupSessionEvent

    data object OnPickerDismissed : GroupSessionEvent

    data class OnClientPicked(val clientUserId: String) : GroupSessionEvent

    data class OnParticipantRemoved(val clientUserId: String) : GroupSessionEvent

    data class OnParticipantOpened(val clientUserId: String) : GroupSessionEvent

    data object OnCompleteClicked : GroupSessionEvent

    data object OnCancelClicked : GroupSessionEvent
}

sealed interface GroupSessionSideEffect {

    data class OpenClientCard(val clientUserId: String) : GroupSessionSideEffect

    data object SessionChanged : GroupSessionSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : GroupSessionSideEffect
}
