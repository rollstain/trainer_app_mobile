package app.trainer.feature.traininglog.presentation.library.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.traininglog.Exercise
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.exercise_library_bodyweight_label
import app.trainer.strings.exercise_library_cardio_label
import app.trainer.strings.exercise_library_strength_label
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.getString

private const val DETAILS_SEPARATOR = " · "

class ExerciseLibraryScreenModel(
    private val trainingLogRepository: TrainingLogRepository,
) : BaseScreenModel<ExerciseLibraryState, ExerciseLibrarySideEffect, ExerciseLibraryEvent>(
    initialState = ExerciseLibraryState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = trainingLogRepository.availableExercises()) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
                    postSideEffect(ExerciseLibrarySideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> {
                    val rows = loaded.data.map { toRow(it) }
                    updateState { current ->
                        current.copy(
                            exercises = rows.toImmutableList(),
                            isLoading = false,
                            failure = null,
                        )
                    }
                }
            }
        }
    }

    override fun dispatch(event: ExerciseLibraryEvent) {
        when (event) {
            ExerciseLibraryEvent.OnReloadRequested -> onFetchData()
            ExerciseLibraryEvent.OnCreateClicked -> screenModelScope {
                postSideEffect(ExerciseLibrarySideEffect.OpenExerciseCreation)
            }
        }
    }

    private suspend fun toRow(exercise: Exercise): ExerciseRow {
        val kindLabel = when (exercise.kind) {
            ExerciseKind.STRENGTH -> getString(Res.string.exercise_library_strength_label)
            ExerciseKind.CARDIO -> getString(Res.string.exercise_library_cardio_label)
            ExerciseKind.BODYWEIGHT -> getString(Res.string.exercise_library_bodyweight_label)
        }
        val muscleGroup = exercise.muscleGroup
        return ExerciseRow(
            exerciseId = exercise.id,
            name = exercise.name,
            details = if (muscleGroup.isNullOrEmpty()) {
                kindLabel
            } else {
                "$muscleGroup$DETAILS_SEPARATOR$kindLabel"
            },
            description = exercise.description,
            video = exercise.videoUrl
                ?.takeIf { it.isNotBlank() }
                ?.let(ExerciseVideo::Link)
                ?: ExerciseVideo.None,
        )
    }
}
