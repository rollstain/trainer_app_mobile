package app.trainer.feature.traininglog.presentation.programeditor.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface DayContent {

    data object Empty : DayContent

    data class Filled(val title: String, val summary: String) : DayContent
}

data class DayRow(
    val dayOfWeek: Int,
    val label: String,
    val content: DayContent,
)

data class ProgramEditorState(
    val programId: String,
    val title: String,
    val weeksCount: Int,
    val selectedWeek: Int,
    val days: ImmutableList<DayRow>,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(programId: String): ProgramEditorState = ProgramEditorState(
            programId = programId,
            title = "",
            weeksCount = 1,
            selectedWeek = 1,
            days = persistentListOf(),
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface ProgramEditorEvent {

    data object OnRetryClicked : ProgramEditorEvent

    data class OnWeekSelected(val weekNumber: Int) : ProgramEditorEvent

    data class OnDayClicked(val dayOfWeek: Int) : ProgramEditorEvent
}

sealed interface ProgramEditorSideEffect {

    data class OpenDay(
        val programId: String,
        val weekNumber: Int,
        val dayOfWeek: Int,
    ) : ProgramEditorSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ProgramEditorSideEffect
}
