package app.trainer.feature.traininglog.presentation.programday.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ExerciseChoice(
    val exerciseId: String,
    val name: String,
)

data class ExerciseLineRow(
    val exerciseId: String,
    val exerciseName: String,
    val setsText: String,
    val repetitionsText: String,
    val weightText: String,
)

data class ProgramDayState(
    val programId: String,
    val weekNumber: Int,
    val dayOfWeek: Int,
    val dayLabel: String,
    val title: String,
    val lines: ImmutableList<ExerciseLineRow>,
    val choices: ImmutableList<ExerciseChoice>,
    val isSaving: Boolean,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val isSaveEnabled: Boolean
        get() = !isSaving && lines.all { it.setsText.toIntOrNull() != null }

    companion object {

        fun initial(programId: String, weekNumber: Int, dayOfWeek: Int): ProgramDayState = ProgramDayState(
            programId = programId,
            weekNumber = weekNumber,
            dayOfWeek = dayOfWeek,
            dayLabel = "",
            title = "",
            lines = persistentListOf(),
            choices = persistentListOf(),
            isSaving = false,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface ProgramDayEvent {

    data object OnRetryClicked : ProgramDayEvent

    data object OnSaveClicked : ProgramDayEvent

    data class OnTitleChanged(val title: String) : ProgramDayEvent

    data class OnExerciseAdded(val exerciseId: String) : ProgramDayEvent

    data class OnLineRemoved(val index: Int) : ProgramDayEvent

    data class OnSetsChanged(val index: Int, val text: String) : ProgramDayEvent

    data class OnRepetitionsChanged(val index: Int, val text: String) : ProgramDayEvent

    data class OnWeightChanged(val index: Int, val text: String) : ProgramDayEvent
}

sealed interface ProgramDaySideEffect {

    data object Saved : ProgramDaySideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ProgramDaySideEffect
}
