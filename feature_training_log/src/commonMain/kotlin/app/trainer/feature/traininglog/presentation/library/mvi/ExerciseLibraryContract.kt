package app.trainer.feature.traininglog.presentation.library.mvi

import app.trainer.entities.RequestResult
import app.trainer.media.PickedMedia
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

sealed interface ExerciseVideo {

    data object None : ExerciseVideo

    data class Link(val url: String) : ExerciseVideo

    data class Uploaded(val url: String) : ExerciseVideo
}

data class ExerciseRow(
    val exerciseId: String,
    val name: String,
    val details: String,
    val author: String?,
    val description: String?,
    val video: ExerciseVideo,
    val isOwnedByMe: Boolean,
    val isUploadingVideo: Boolean,
)

data class ExerciseLibraryState(
    val exercises: ImmutableList<ExerciseRow>,
    val nextCursor: String?,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasMore: Boolean
        get() = nextCursor != null

    fun withFirstPage(rows: List<ExerciseRow>, nextCursor: String?): ExerciseLibraryState = copy(
        exercises = rows.toImmutableList(),
        nextCursor = nextCursor,
        isLoading = false,
        isLoadingMore = false,
        failure = null,
    )

    fun withNextPage(rows: List<ExerciseRow>, nextCursor: String?): ExerciseLibraryState {
        val known = exercises.mapTo(mutableSetOf(), ExerciseRow::exerciseId)
        val fresh = rows.filterNot { it.exerciseId in known }
        return copy(
            exercises = (exercises + fresh).toImmutableList(),
            nextCursor = nextCursor,
            isLoadingMore = false,
        )
    }

    companion object {

        fun initial(): ExerciseLibraryState = ExerciseLibraryState(
            exercises = persistentListOf(),
            nextCursor = null,
            isLoading = true,
            isLoadingMore = false,
            failure = null,
        )
    }
}

sealed interface ExerciseLibraryEvent {

    data object OnReloadRequested : ExerciseLibraryEvent

    data object OnEndReached : ExerciseLibraryEvent

    data object OnCreateClicked : ExerciseLibraryEvent

    data class OnVideoPicked(val exerciseId: String, val video: PickedMedia) : ExerciseLibraryEvent

    data class OnArchiveClicked(val exerciseId: String) : ExerciseLibraryEvent
}

sealed interface ExerciseLibrarySideEffect {

    data object OpenExerciseCreation : ExerciseLibrarySideEffect

    data object ShowVideoUploaded : ExerciseLibrarySideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ExerciseLibrarySideEffect
}
