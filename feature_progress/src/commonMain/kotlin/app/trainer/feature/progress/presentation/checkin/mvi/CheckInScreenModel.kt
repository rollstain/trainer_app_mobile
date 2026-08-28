package app.trainer.feature.progress.presentation.checkin.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.monthGenitiveOf
import app.trainer.base.input.WeightInput
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInDraft
import app.trainer.data.progress.CheckInRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.media.PickedMedia
import app.trainer.strings.Res
import app.trainer.strings.check_in_girth_not_a_number
import app.trainer.strings.check_in_girth_out_of_range
import app.trainer.strings.check_in_weight_not_a_number
import app.trainer.strings.check_in_weight_out_of_range
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

private const val MILLIMETERS_IN_CENTIMETER = 10
private const val MIN_BODY_WEIGHT_GRAMS = 20_000
private const val MAX_BODY_WEIGHT_GRAMS = 400_000
private const val MIN_BODY_GIRTH_MILLIMETERS = 200
private const val MAX_BODY_GIRTH_MILLIMETERS = 3_000

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
            updateState { it.copy(isLoading = true, failure = null) }
            val loaded = checkInRepository.ownCheckIns(from = checkInDate, to = checkInDate)
            when (loaded) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
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
            is CheckInEvent.OnAdherenceSelected -> updateState { it.copy(adherence = event.rating) }
            is CheckInEvent.OnNotesChanged -> updateState { it.copy(notes = event.notes) }
            is CheckInEvent.OnPhotoPicked -> uploadPhoto(event.image)
            is CheckInEvent.OnPhotoRemoved -> removePhoto(event.photoId)
        }
    }

    private suspend fun showLoaded(checkIn: CheckIn?) {
        val dateLabel = formatDate(checkInDate)
        updateState { current ->
            current.copy(
                dateLabel = dateLabel,
                weightText = checkIn?.weightGrams?.let(weightInput::toKilogramsText).orEmpty(),
                waistText = toCentimetersText(checkIn?.waistMillimeters),
                chestText = toCentimetersText(checkIn?.chestMillimeters),
                hipsText = toCentimetersText(checkIn?.hipsMillimeters),
                wellbeing = checkIn?.wellbeing,
                adherence = checkIn?.adherence,
                sleepQuality = checkIn?.sleepQuality,
                notes = checkIn?.notes.orEmpty(),
                photos = checkIn?.photos.orEmpty()
                    .map { photo -> PhotoRow(photoId = photo.id, url = photo.downloadUrl) }
                    .toImmutableList(),
                isLoading = false,
                failure = null,
            )
        }
    }

    private fun save() {
        screenModelScope { state ->
            if (!state.isSaveEnabled) return@screenModelScope
            val weightGrams = weightInput.toGrams(state.weightText)
            val waistMillimeters = toMillimeters(state.waistText)
            val chestMillimeters = toMillimeters(state.chestText)
            val hipsMillimeters = toMillimeters(state.hipsText)
            val rejection = rejectWeight(text = state.weightText, grams = weightGrams)
                ?: rejectGirth(text = state.waistText, millimeters = waistMillimeters)
                ?: rejectGirth(text = state.chestText, millimeters = chestMillimeters)
                ?: rejectGirth(text = state.hipsText, millimeters = hipsMillimeters)
            if (rejection != null) {
                postSideEffect(CheckInSideEffect.ShowFailure(rejection))
                return@screenModelScope
            }

            updateState { it.copy(isSaving = true) }
            val saved = checkInRepository.save(
                checkInDate = checkInDate,
                draft = CheckInDraft(
                    weightGrams = weightGrams,
                    waistMillimeters = waistMillimeters,
                    chestMillimeters = chestMillimeters,
                    hipsMillimeters = hipsMillimeters,
                    wellbeing = state.wellbeing,
                    sleepQuality = state.sleepQuality,
                    adherence = state.adherence,
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

    private fun uploadPhoto(image: PickedMedia) {
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

    private suspend fun rejectWeight(text: String, grams: Int?): RequestResult.Error? {
        if (text.isBlank()) return null
        if (grams == null) {
            return RequestResult.Error(
                kind = RequestFailure.Validation,
                statusCode = null,
                userMessage = getString(Res.string.check_in_weight_not_a_number),
                devMessage = "Не разобран вес $text",
            )
        }
        if (grams < MIN_BODY_WEIGHT_GRAMS || grams > MAX_BODY_WEIGHT_GRAMS) {
            val minKilograms = weightInput.toKilogramsText(MIN_BODY_WEIGHT_GRAMS)
            val maxKilograms = weightInput.toKilogramsText(MAX_BODY_WEIGHT_GRAMS)
            return RequestResult.Error(
                kind = RequestFailure.Validation,
                statusCode = null,
                userMessage = getString(Res.string.check_in_weight_out_of_range, minKilograms, maxKilograms),
                devMessage = "Вес вне допустимого диапазона: $grams г",
            )
        }
        return null
    }

    private suspend fun rejectGirth(text: String, millimeters: Int?): RequestResult.Error? {
        if (text.isBlank()) return null
        if (millimeters == null) {
            return RequestResult.Error(
                kind = RequestFailure.Validation,
                statusCode = null,
                userMessage = getString(Res.string.check_in_girth_not_a_number),
                devMessage = "Не разобран обхват $text",
            )
        }
        if (millimeters < MIN_BODY_GIRTH_MILLIMETERS || millimeters > MAX_BODY_GIRTH_MILLIMETERS) {
            return RequestResult.Error(
                kind = RequestFailure.Validation,
                statusCode = null,
                userMessage = getString(
                    Res.string.check_in_girth_out_of_range,
                    MIN_BODY_GIRTH_MILLIMETERS / MILLIMETERS_IN_CENTIMETER,
                    MAX_BODY_GIRTH_MILLIMETERS / MILLIMETERS_IN_CENTIMETER,
                ),
                devMessage = "Обхват вне допустимого диапазона: $millimeters мм",
            )
        }
        return null
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

    private suspend fun formatDate(date: LocalDate): String {
        val month = monthGenitiveOf(date)
        return "${date.day} $month"
    }
}
