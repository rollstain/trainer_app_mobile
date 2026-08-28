package app.trainer.feature.traininglog.presentation.library.mvi

import app.trainer.data.traininglog.Equipment
import app.trainer.data.traininglog.ExerciseOwnerKind
import app.trainer.data.traininglog.MuscleGroup
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

data class ExerciseFilter(
    val muscles: ImmutableList<MuscleGroup>,
    val equipment: ImmutableList<Equipment>,
    val ownerKind: ExerciseOwnerKind?,
) {

    val appliedCount: Int
        get() = muscles.size + equipment.size + (if (ownerKind == null) 0 else 1)

    val isEmpty: Boolean
        get() = appliedCount == 0

    companion object {

        fun empty(): ExerciseFilter = ExerciseFilter(persistentListOf(), persistentListOf(), null)
    }
}

data class ExerciseLibraryState(
    val exercises: ImmutableList<ExerciseRow>,
    val nextCursor: String?,
    val search: String,
    val filter: ExerciseFilter,
    val draftFilter: ExerciseFilter?,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasMore: Boolean
        get() = nextCursor != null

    val isFiltered: Boolean
        get() = search.isNotBlank() || !filter.isEmpty

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
            search = "",
            filter = ExerciseFilter.empty(),
            draftFilter = null,
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

    data class OnSearchChanged(val query: String) : ExerciseLibraryEvent

    data object OnFilterOpened : ExerciseLibraryEvent

    data object OnFilterDismissed : ExerciseLibraryEvent

    data object OnFilterCleared : ExerciseLibraryEvent

    data object OnFilterApplied : ExerciseLibraryEvent

    data class OnMuscleToggled(val muscle: MuscleGroup) : ExerciseLibraryEvent

    data class OnEquipmentToggled(val equipment: Equipment) : ExerciseLibraryEvent

    data class OnOwnerKindChanged(val ownerKind: ExerciseOwnerKind?) : ExerciseLibraryEvent
}

sealed interface ExerciseLibrarySideEffect {

    data object OpenExerciseCreation : ExerciseLibrarySideEffect

    data object ShowVideoUploaded : ExerciseLibrarySideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ExerciseLibrarySideEffect
}
