package app.trainer.feature.clientcard.presentation.mvi

import app.trainer.base.metrics.MetricChart
import app.trainer.base.metrics.ProgressMetric
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

sealed interface CheckInReview {

    data object Awaiting : CheckInReview

    data class Answered(val comment: String?) : CheckInReview
}

data class CheckInRow(
    val checkInId: String,
    val dateLabel: String,
    val measurements: String,
    val wellbeingLabel: String,
    val notes: String?,
    val review: CheckInReview,
    val photos: ImmutableList<CheckInPhotoRow>,
)

data class ReviewEditor(
    val checkInId: String,
    val comment: String,
    val isSaving: Boolean,
)

data class ClientHabitRow(
    val habitId: String,
    val title: String,
    val doneCountLabel: String,
    val isSetByCoach: Boolean,
)

sealed interface ClientProgramState {

    data object None : ClientProgramState

    data class Assigned(val title: String, val startsLabel: String) : ClientProgramState
}

enum class ProgramStart { Today, NextMonday }

data class ProgramPickRow(
    val programId: String,
    val title: String,
    val summary: String,
)

data class ProgramPicker(
    val programs: ImmutableList<ProgramPickRow>,
    val startsOn: ProgramStart,
    val nextCursor: String?,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val isSaving: Boolean,
) {

    val hasMore: Boolean
        get() = nextCursor != null
}

enum class ClientCardTab { Now, Metrics, History }

data class ClientCardState(
    val clientUserId: String,
    val tab: ClientCardTab,
    val notes: ImmutableList<NoteRow>,
    val checkIns: ImmutableList<CheckInRow>,
    val charts: ImmutableList<MetricChart>,
    val selectedMetric: ProgressMetric?,
    val habits: ImmutableList<ClientHabitRow>,
    val newHabitTitle: String,
    val editor: NoteEditor?,
    val program: ClientProgramState,
    val programPicker: ProgramPicker?,
    val reviewEditor: ReviewEditor?,
    val isArchiveDialogVisible: Boolean,
    val isArchiving: Boolean,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val isAddHabitEnabled: Boolean
        get() = newHabitTitle.isNotBlank()

    val selectedChart: MetricChart?
        get() = charts.firstOrNull { it.metric == selectedMetric }

    val isEmptyCard: Boolean
        get() = notes.isEmpty() && checkIns.isEmpty() && habits.isEmpty()

    companion object {

        fun initial(clientUserId: String): ClientCardState = ClientCardState(
            clientUserId = clientUserId,
            tab = ClientCardTab.Now,
            notes = persistentListOf(),
            checkIns = persistentListOf(),
            charts = persistentListOf(),
            selectedMetric = null,
            habits = persistentListOf(),
            newHabitTitle = "",
            editor = null,
            program = ClientProgramState.None,
            programPicker = null,
            reviewEditor = null,
            isArchiveDialogVisible = false,
            isArchiving = false,
            isLoading = true,
            failure = null,
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

    data class OnTabSelected(val tab: ClientCardTab) : ClientCardEvent

    data object OnOpenDiaryClicked : ClientCardEvent

    data class OnMetricSelected(val metric: ProgressMetric) : ClientCardEvent

    data class OnReviewClicked(val checkInId: String) : ClientCardEvent

    data object OnComparePhotosClicked : ClientCardEvent

    data object OnReviewDismissed : ClientCardEvent

    data object OnReviewSaveClicked : ClientCardEvent

    data class OnReviewCommentChanged(val comment: String) : ClientCardEvent

    data object OnAssignProgramClicked : ClientCardEvent

    data object OnProgramPickerMoreClicked : ClientCardEvent

    data object OnProgramPickerDismissed : ClientCardEvent

    data object OnProgramRemoved : ClientCardEvent

    data class OnProgramStartSelected(val start: ProgramStart) : ClientCardEvent

    data class OnProgramPicked(val programId: String) : ClientCardEvent

    data object OnArchiveClientClicked : ClientCardEvent

    data object OnArchiveConfirmed : ClientCardEvent

    data object OnArchiveDismissed : ClientCardEvent
}

sealed interface ClientCardSideEffect {

    data class OpenDiary(val clientUserId: String) : ClientCardSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ClientCardSideEffect

    data object ShowNoteArchived : ClientCardSideEffect

    data object ClientArchived : ClientCardSideEffect

    data class OpenPhotoCompare(val clientUserId: String) : ClientCardSideEffect
}
