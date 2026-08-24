package app.trainer.feature.progress.presentation.checkin.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.input.WeightInput
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInDraft
import app.trainer.data.progress.CheckInRepository
import app.trainer.entities.RequestResult
import app.trainer.media.PickedImage
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate

private const val MILLIMETERS_IN_CENTIMETER = 10

private val MONTH_NAMES = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

class CheckInScreenModel(
    dateIso: String,
    private val checkInRepository: CheckInRepository,
    private val weightInput: WeightInput,
) : BaseScreenModel<CheckInState, CheckInSideEffect, CheckInEvent>(
    initialState = CheckInState.initial(),
) {

    private val checkInDate: LocalDate = LocalDate.parse(dateIso)

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, isFailed = false) }
            val loaded = checkInRepository.ownCheckIns(from = checkInDate, to = checkInDate)
            when (loaded) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, isFailed = true) }
                    postSideEffect(CheckInSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> showLoaded(loaded.data.firstOrNull())
            }
        }
    }

    override fun dispatch(event: CheckInEvent) {
        when (event) {
            CheckInEvent.OnRetryClicked -> onFetchData()
            CheckInEvent.OnSaveClicked -> save()
            is CheckInEvent.OnWeightChanged -> updateState { it.copy(weightText = event.text) }
            is CheckInEvent.OnWaistChanged -> updateState {
                it.copy(waistText = event.text.filter(Char::isDigit))
            }
            is CheckInEvent.OnChestChanged -> updateState {
                it.copy(chestText = event.text.filter(Char::isDigit))
            }
            is CheckInEvent.OnHipsChanged -> updateState {
                it.copy(hipsText = event.text.filter(Char::isDigit))
            }
            is CheckInEvent.OnWellbeingSelected -> updateState { it.copy(wellbeing = event.rating) }
            is CheckInEvent.OnSleepQualitySelected -> updateState { it.copy(sleepQuality = event.rating) }
            is CheckInEvent.OnNotesChanged -> updateState { it.copy(notes = event.notes) }
            is CheckInEvent.OnPhotoPicked -> uploadPhoto(event.image)
            is CheckInEvent.OnPhotoRemoved -> removePhoto(event.photoId)
        }
    }

    private suspend fun showLoaded(checkIn: CheckIn?) {
        updateState { current ->
            current.copy(
                dateLabel = formatDate(checkInDate),
                weightText = checkIn?.weightGrams?.let(weightInput::toKilogramsText).orEmpty(),
                waistText = toCentimetersText(checkIn?.waistMillimeters),
                chestText = toCentimetersText(checkIn?.chestMillimeters),
                hipsText = toCentimetersText(checkIn?.hipsMillimeters),
                wellbeing = checkIn?.wellbeing,
                sleepQuality = checkIn?.sleepQuality,
                notes = checkIn?.notes.orEmpty(),
                photos = checkIn?.photos.orEmpty()
                    .map { photo -> PhotoRow(photoId = photo.id, url = photo.downloadUrl) }
                    .toImmutableList(),
                isLoading = false,
                isFailed = false,
            )
        }
    }

    private fun save() {
        screenModelScope { state ->
            if (!state.isSaveEnabled) return@screenModelScope
            val weightGrams = weightInput.toGrams(state.weightText)
            if (state.weightText.isNotBlank() && weightGrams == null) {
                postSideEffect(
                    CheckInSideEffect.ShowFailure(
                        RequestResult.Error(
                            statusCode = null,
                            userMessage = "Проверьте вес: нужно число, например 82,4",
                            devMessage = "Не разобран вес ${state.weightText}",
                        )
                    )
                )
                return@screenModelScope
            }

            updateState { it.copy(isSaving = true) }
            val saved = checkInRepository.save(
                checkInDate = checkInDate,
                draft = CheckInDraft(
                    weightGrams = weightGrams,
                    waistMillimeters = toMillimeters(state.waistText),
                    chestMillimeters = toMillimeters(state.chestText),
                    hipsMillimeters = toMillimeters(state.hipsText),
                    wellbeing = state.wellbeing,
                    sleepQuality = state.sleepQuality,
                    notes = state.notes.trim().ifEmpty { null },
                    photoIds = state.photos.map { it.photoId },
                ),
            )
            updateState { it.copy(isSaving = false) }
            when (saved) {
                is RequestResult.Error -> postSideEffect(CheckInSideEffect.ShowFailure(saved))
                is RequestResult.Success -> postSideEffect(CheckInSideEffect.ShowSaved)
            }
        }
    }

    private fun uploadPhoto(image: PickedImage) {
        screenModelScope { state ->
            if (!state.canAddPhoto) return@screenModelScope
            updateState { it.copy(isUploadingPhoto = true) }
            val prepared = checkInRepository.preparePhotoUpload(
                fileName = image.fileName,
                contentType = image.contentType,
                sizeBytes = image.bytes.size.toLong(),
            )
            if (prepared is RequestResult.Error) {
                updateState { it.copy(isUploadingPhoto = false) }
                postSideEffect(CheckInSideEffect.ShowFailure(prepared))
                return@screenModelScope
            }
            val upload = (prepared as RequestResult.Success).data
            val uploaded = checkInRepository.uploadPhoto(
                uploadUrl = upload.uploadUrl,
                contentType = image.contentType,
                bytes = image.bytes,
            )
            updateState { it.copy(isUploadingPhoto = false) }
            when (uploaded) {
                is RequestResult.Error -> postSideEffect(CheckInSideEffect.ShowFailure(uploaded))
                is RequestResult.Success -> updateState { current ->
                    current.copy(
                        photos = (
                            current.photos + PhotoRow(
                                photoId = upload.photoId,
                                url = upload.downloadUrl,
                            )
                            ).toImmutableList(),
                    )
                }
            }
        }
    }

    private fun removePhoto(photoId: String) {
        screenModelScope {
            when (val removed = checkInRepository.deletePhoto(photoId = photoId)) {
                is RequestResult.Error -> postSideEffect(CheckInSideEffect.ShowFailure(removed))
                is RequestResult.Success -> updateState { current ->
                    current.copy(
                        photos = current.photos.filterNot { it.photoId == photoId }.toImmutableList(),
                    )
                }
            }
        }
    }

    private fun toMillimeters(centimetersText: String): Int? {
        val centimeters = centimetersText.trim().toIntOrNull() ?: return null
        if (centimeters <= 0) return null
        return centimeters * MILLIMETERS_IN_CENTIMETER
    }

    private fun toCentimetersText(millimeters: Int?): String {
        if (millimeters == null) return ""
        return (millimeters / MILLIMETERS_IN_CENTIMETER).toString()
    }

    private fun formatDate(date: LocalDate): String {
        val month = MONTH_NAMES[date.monthNumber - 1]
        return "${date.dayOfMonth} $month"
    }
}
