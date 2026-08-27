package app.trainer.feature.traininglog.presentation.programs.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val MIN_WEEKS = 1
private const val MAX_WEEKS = 12

data class ProgramRow(
    val programId: String,
    val title: String,
    val summary: String,
)

data class NewProgramDraft(
    val title: String,
    val weeksCount: Int,
    val isSaving: Boolean,
) {

    val isSaveEnabled: Boolean
        get() = title.isNotBlank() && !isSaving

    val canAddWeek: Boolean
        get() = weeksCount < MAX_WEEKS

    val canRemoveWeek: Boolean
        get() = weeksCount > MIN_WEEKS

    companion object {

        fun empty(): NewProgramDraft = NewProgramDraft(title = "", weeksCount = 4, isSaving = false)
    }
}

data class ProgramsState(
    val programs: ImmutableList<ProgramRow>,
    val draft: NewProgramDraft?,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(): ProgramsState = ProgramsState(
            programs = persistentListOf(),
            draft = null,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface ProgramsEvent {

    data object OnRetryClicked : ProgramsEvent

    data object OnCreateClicked : ProgramsEvent

    data object OnDraftDismissed : ProgramsEvent

    data object OnDraftSaveClicked : ProgramsEvent

    data object OnWeekAdded : ProgramsEvent

    data object OnWeekRemoved : ProgramsEvent

    data class OnDraftTitleChanged(val title: String) : ProgramsEvent

    data class OnProgramClicked(val programId: String) : ProgramsEvent

    data class OnProgramArchived(val programId: String) : ProgramsEvent

    data class OnProgramDuplicated(val programId: String) : ProgramsEvent
}

sealed interface ProgramsSideEffect {

    data class OpenProgram(val programId: String) : ProgramsSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ProgramsSideEffect
}
