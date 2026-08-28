package app.trainer.feature.progress.presentation.photos.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.monthGenitiveOf
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.photo_compare_date
import app.trainer.strings.photo_compare_date_with_year
import kotlin.time.Clock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.getString

private const val PHOTO_HISTORY_DAYS = 365

class PhotoCompareScreenModel(
    private val owner: PhotoOwner,
    private val checkInRepository: CheckInRepository,
) : BaseScreenModel<PhotoCompareState, PhotoCompareSideEffect, PhotoCompareEvent>(
    initialState = PhotoCompareState.initial(),
) {

    private val today: LocalDate get() = Clock.System.todayIn(TimeZone.currentSystemDefault())

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            val from = today.minus(DatePeriod(days = PHOTO_HISTORY_DAYS))
            val loaded = when (owner) {
                PhotoOwner.Own -> checkInRepository.ownCheckIns(from = from, to = today)
                is PhotoOwner.Client -> checkInRepository.clientCheckIns(
                    clientUserId = owner.userId,
                    from = from,
                    to = today,
                )
            }
            when (loaded) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
                    postSideEffect(PhotoCompareSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> show(loaded.data)
            }
        }
    }

    override fun dispatch(event: PhotoCompareEvent) {
        when (event) {
            PhotoCompareEvent.OnReloadRequested -> onFetchData()
            is PhotoCompareEvent.OnSideSelected -> updateState { it.copy(selectedSide = event.side) }
            is PhotoCompareEvent.OnShotPicked -> pickShot(event.photoId)
        }
    }

    private suspend fun show(checkIns: List<CheckIn>) {
        val shots = shotsOf(checkIns)
        updateState { current ->
            current.copy(
                shots = shots.toImmutableList(),
                beforePhotoId = shots.firstOrNull()?.photoId,
                afterPhotoId = shots.lastOrNull()?.photoId,
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun shotsOf(checkIns: List<CheckIn>): List<PhotoShot> {
        return checkIns
            .sortedBy { it.checkInDate }
            .flatMap { checkIn -> checkIn.photos.map { photo -> checkIn.checkInDate to photo } }
            .map { (date, photo) ->
                PhotoShot(
                    photoId = photo.id,
                    url = photo.downloadUrl,
                    dateIso = date.toString(),
                    dateLabel = dateLabelOf(date),
                )
            }
    }

    private suspend fun dateLabelOf(date: LocalDate): String {
        val month = monthGenitiveOf(date)
        return if (date.year == today.year) {
            getString(Res.string.photo_compare_date, date.day, month)
        } else {
            getString(Res.string.photo_compare_date_with_year, date.day, month, date.year)
        }
    }

    private fun pickShot(photoId: String) {
        updateState { it.withShotPicked(photoId) }
    }
}
