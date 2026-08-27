package app.trainer.feature.traininglog.presentation.library.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface ExerciseVideo {

    data object None : ExerciseVideo

    data class Link(val url: String) : ExerciseVideo
}

data class ExerciseRow(
    val exerciseId: String,
    val name: String,
    val details: String,
    val description: String?,
    val video: ExerciseVideo,
)

data class ExerciseLibraryState(
    val exercises: ImmutableList<ExerciseRow>,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(): ExerciseLibraryState = ExerciseLibraryState(
            exercises = persistentListOf(),
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface ExerciseLibraryEvent {

    data object OnReloadRequested : ExerciseLibraryEvent

    data object OnCreateClicked : ExerciseLibraryEvent
}

sealed interface ExerciseLibrarySideEffect {

    data object OpenExerciseCreation : ExerciseLibrarySideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ExerciseLibrarySideEffect
}
