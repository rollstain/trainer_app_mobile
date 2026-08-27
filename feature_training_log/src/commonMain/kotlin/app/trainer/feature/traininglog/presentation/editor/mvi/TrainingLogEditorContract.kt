package app.trainer.feature.traininglog.presentation.editor.mvi

import app.trainer.data.traininglog.ExerciseKind
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

data class LastResultHints(
    val repetitions: String,
    val weight: String,
    val duration: String,
    val distance: String,
) {

    companion object {

        val Empty = LastResultHints(repetitions = "", weight = "", duration = "", distance = "")
    }
}

data class ExerciseOption(
    val exerciseId: String,
    val name: String,
    val muscleGroup: String?,
    val kind: ExerciseKind,
    val lastResult: LastResultHints,
)

data class SetRow(
    val rowId: String,
    val exerciseId: String,
    val exerciseName: String,
    val kind: ExerciseKind,
    val repetitionsText: String,
    val weightText: String,
    val durationText: String,
    val distanceText: String,
    val lastResult: LastResultHints,
    val isPersonalRecord: Boolean,
) {

    val isFilled: Boolean
        get() = when (kind) {
            ExerciseKind.STRENGTH -> repetitionsText.isNotBlank() && weightText.isNotBlank()
            ExerciseKind.BODYWEIGHT -> repetitionsText.isNotBlank()
            ExerciseKind.CARDIO -> durationText.isNotBlank() || distanceText.isNotBlank()
        }
}

data class RestState(val label: String, val progress: Float)

sealed interface PlannedForDay {

    data object None : PlannedForDay

    data class Workout(val dayTitle: String, val summary: String) : PlannedForDay
}

data class TrainingLogEditorState(
    val entryDate: LocalDate,
    val dateLabel: String,
    val volumeLabel: String,
    val exercises: ImmutableList<ExerciseOption>,
    val sets: ImmutableList<SetRow>,
    val notes: String,
    val rest: RestState?,
    val planned: PlannedForDay,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val isQueued: Boolean,
    val failure: RequestResult.Error?,
) {

    val isSaveEnabled: Boolean
        get() = !isSaving && sets.isNotEmpty() && sets.all { it.isFilled }

    companion object {

        fun initial(entryDate: LocalDate): TrainingLogEditorState = TrainingLogEditorState(
            entryDate = entryDate,
            dateLabel = "",
            volumeLabel = "",
            exercises = persistentListOf(),
            sets = persistentListOf(),
            notes = "",
            rest = null,
            planned = PlannedForDay.None,
            isLoading = true,
            isSaving = false,
            isQueued = false,
            failure = null,
        )
    }
}

sealed interface TrainingLogEditorEvent {

    data object OnRetryClicked : TrainingLogEditorEvent

    data object OnPlanApplied : TrainingLogEditorEvent

    data object OnSaveClicked : TrainingLogEditorEvent

    data class OnExerciseAdded(val exerciseId: String) : TrainingLogEditorEvent

    data class OnSetRemoved(val rowId: String) : TrainingLogEditorEvent

    data class OnSetDuplicated(val rowId: String) : TrainingLogEditorEvent

    data class OnRepetitionsChanged(val rowId: String, val text: String) : TrainingLogEditorEvent

    data class OnWeightChanged(val rowId: String, val text: String) : TrainingLogEditorEvent

    data class OnDurationChanged(val rowId: String, val text: String) : TrainingLogEditorEvent

    data class OnDistanceChanged(val rowId: String, val text: String) : TrainingLogEditorEvent

    data class OnNotesChanged(val notes: String) : TrainingLogEditorEvent

    data object OnRestExtended : TrainingLogEditorEvent

    data object OnRestSkipped : TrainingLogEditorEvent
}

sealed interface TrainingLogEditorSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : TrainingLogEditorSideEffect

    data object ShowSaved : TrainingLogEditorSideEffect

    data object ShowQueued : TrainingLogEditorSideEffect
}
