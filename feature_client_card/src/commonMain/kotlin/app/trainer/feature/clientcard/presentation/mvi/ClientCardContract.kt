package app.trainer.feature.clientcard.presentation.mvi

import app.trainer.data.clients.ClientNoteKind
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class NoteRow(
    val noteId: String,
    val kind: ClientNoteKind,
    val title: String,
    val details: String?,
    val isPinned: Boolean,
    val updatedAtLabel: String,
)

data class NoteEditor(
    val editedNoteId: String?,
    val kind: ClientNoteKind,
    val title: String,
    val details: String,
    val isPinned: Boolean,
    val isSaving: Boolean,
) {

    val isSaveEnabled: Boolean
        get() = title.isNotBlank() && !isSaving

    companion object {

        fun forNewNote(): NoteEditor = NoteEditor(
            editedNoteId = null,
            kind = ClientNoteKind.MEDICAL,
            title = "",
            details = "",
            isPinned = true,
            isSaving = false,
        )
    }
}

data class CheckInPhotoRow(
    val photoId: String,
    val url: String,
)

data class CheckInRow(
    val checkInId: String,
    val dateLabel: String,
    val measurements: String,
    val wellbeingLabel: String,
    val notes: String?,
    val photos: ImmutableList<CheckInPhotoRow>,
)

data class ClientHabitRow(
    val habitId: String,
    val title: String,
    val doneCountLabel: String,
    val isSetByCoach: Boolean,
)

data class ClientCardState(
    val clientUserId: String,
    val notes: ImmutableList<NoteRow>,
    val checkIns: ImmutableList<CheckInRow>,
    val habits: ImmutableList<ClientHabitRow>,
    val newHabitTitle: String,
    val editor: NoteEditor?,
    val isLoading: Boolean,
    val isFailed: Boolean,
) {

    val isAddHabitEnabled: Boolean
        get() = newHabitTitle.isNotBlank()

    val isEmptyCard: Boolean
        get() = notes.isEmpty() && checkIns.isEmpty() && habits.isEmpty()

    companion object {

        fun initial(clientUserId: String): ClientCardState = ClientCardState(
            clientUserId = clientUserId,
            notes = persistentListOf(),
            checkIns = persistentListOf(),
            habits = persistentListOf(),
            newHabitTitle = "",
            editor = null,
            isLoading = true,
            isFailed = false,
        )
    }
}

sealed interface ClientCardEvent {

    data object OnRetryClicked : ClientCardEvent

    data object OnAddNoteClicked : ClientCardEvent

    data object OnEditorDismissed : ClientCardEvent

    data object OnEditorSaveClicked : ClientCardEvent

    data object OnEditorPinToggled : ClientCardEvent

    data class OnNoteClicked(val noteId: String) : ClientCardEvent

    data class OnNoteArchived(val noteId: String) : ClientCardEvent

    data class OnEditorTitleChanged(val title: String) : ClientCardEvent

    data class OnEditorDetailsChanged(val details: String) : ClientCardEvent

    data class OnEditorKindChanged(val kind: ClientNoteKind) : ClientCardEvent

    data class OnNewHabitTitleChanged(val title: String) : ClientCardEvent

    data object OnHabitAdded : ClientCardEvent
}

sealed interface ClientCardSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : ClientCardSideEffect

    data object ShowNoteArchived : ClientCardSideEffect
}
