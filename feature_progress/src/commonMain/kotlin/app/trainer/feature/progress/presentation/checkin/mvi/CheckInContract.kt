package app.trainer.feature.progress.presentation.checkin.mvi

import app.trainer.entities.RequestResult
import app.trainer.media.PickedImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val PHOTOS_MAX_COUNT = 6

data class PhotoRow(
    val photoId: String,
    val url: String,
)

data class CheckInState(
    val dateLabel: String,
    val weightText: String,
    val waistText: String,
    val chestText: String,
    val hipsText: String,
    val wellbeing: Int?,
    val sleepQuality: Int?,
    val adherence: Int?,
    val notes: String,
    val photos: ImmutableList<PhotoRow>,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val isUploadingPhoto: Boolean,
    val failure: RequestResult.Error?,
) {

    val canAddPhoto: Boolean
        get() = !isUploadingPhoto && photos.size < PHOTOS_MAX_COUNT

    val isSaveEnabled: Boolean
        get() = !isSaving && (
            weightText.isNotBlank() ||
                waistText.isNotBlank() ||
                chestText.isNotBlank() ||
                hipsText.isNotBlank() ||
                wellbeing != null ||
                sleepQuality != null ||
                adherence != null ||
                notes.isNotBlank() ||
                photos.isNotEmpty()
            )

    companion object {

        fun initial(): CheckInState = CheckInState(
            dateLabel = "",
            weightText = "",
            waistText = "",
            chestText = "",
            hipsText = "",
            wellbeing = null,
            sleepQuality = null,
            adherence = null,
            notes = "",
            photos = persistentListOf(),
            isLoading = true,
            isSaving = false,
            isUploadingPhoto = false,
            failure = null,
        )
    }
}

sealed interface CheckInEvent {

    data object OnRetryClicked : CheckInEvent

    data object OnSaveClicked : CheckInEvent

    data class OnWeightChanged(val text: String) : CheckInEvent

    data class OnWaistChanged(val text: String) : CheckInEvent

    data class OnChestChanged(val text: String) : CheckInEvent

    data class OnHipsChanged(val text: String) : CheckInEvent

    data class OnWellbeingSelected(val rating: Int) : CheckInEvent

    data class OnSleepQualitySelected(val rating: Int) : CheckInEvent

    data class OnAdherenceSelected(val rating: Int) : CheckInEvent

    data class OnNotesChanged(val notes: String) : CheckInEvent

    data class OnPhotoPicked(val image: PickedImage) : CheckInEvent

    data class OnPhotoRemoved(val photoId: String) : CheckInEvent
}

sealed interface CheckInSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : CheckInSideEffect

    data object ShowSaved : CheckInSideEffect
}
