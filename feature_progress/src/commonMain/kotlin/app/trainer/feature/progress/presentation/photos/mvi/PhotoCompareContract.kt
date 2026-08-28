package app.trainer.feature.progress.presentation.photos.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val MIN_SHOTS_TO_COMPARE = 2

sealed interface PhotoOwner {

    data object Own : PhotoOwner

    data class Client(val userId: String) : PhotoOwner
}

enum class CompareSide { Before, After }

data class PhotoShot(
    val photoId: String,
    val url: String,
    val dateIso: String,
    val dateLabel: String,
)

data class PhotoCompareState(
    val shots: ImmutableList<PhotoShot>,
    val beforePhotoId: String?,
    val afterPhotoId: String?,
    val selectedSide: CompareSide,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val before: PhotoShot?
        get() = shots.firstOrNull { it.photoId == beforePhotoId }

    val after: PhotoShot?
        get() = shots.firstOrNull { it.photoId == afterPhotoId }

    val canCompare: Boolean
        get() = shots.size >= MIN_SHOTS_TO_COMPARE

    fun withShotPicked(photoId: String): PhotoCompareState = when (selectedSide) {
        CompareSide.Before -> copy(
            beforePhotoId = photoId,
            afterPhotoId = if (photoId == afterPhotoId) beforePhotoId else afterPhotoId,
        )
        CompareSide.After -> copy(
            afterPhotoId = photoId,
            beforePhotoId = if (photoId == beforePhotoId) afterPhotoId else beforePhotoId,
        )
    }

    companion object {

        fun initial(): PhotoCompareState = PhotoCompareState(
            shots = persistentListOf(),
            beforePhotoId = null,
            afterPhotoId = null,
            selectedSide = CompareSide.After,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface PhotoCompareEvent {

    data object OnReloadRequested : PhotoCompareEvent

    data class OnSideSelected(val side: CompareSide) : PhotoCompareEvent

    data class OnShotPicked(val photoId: String) : PhotoCompareEvent
}

sealed interface PhotoCompareSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : PhotoCompareSideEffect
}
