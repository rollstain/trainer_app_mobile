package app.trainer.feature.schedule.presentation.seriesresult.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.timeOfDayOf
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.schedule.SlotSeriesResult
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.feature.schedule.domain.SlotSeriesResults
import app.trainer.feature.schedule.presentation.formatScheduleDate
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SeriesResultScreenModel(
    private val batchId: String,
    private val seriesResults: SlotSeriesResults,
    private val profileRepository: ProfileRepository,
    private val weeks: ScheduleWeeks,
) : BaseScreenModel<SeriesResultState, SeriesResultSideEffect, SeriesResultEvent>(
    initialState = SeriesResultState.initial(),
) {

    private var takenResult: SlotSeriesResult? = null

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            val result = takenResult ?: seriesResults.take(batchId)
            if (result == null) {
                updateState { it.copy(isExpired = true, isLoading = false) }
                return@onFetchDataScope
            }
            takenResult = result
            val zone = resolveCoachZone() ?: return@onFetchDataScope
            showResult(result = result, zone = zone)
        }
    }

    override fun dispatch(event: SeriesResultEvent) {
        when (event) {
            SeriesResultEvent.OnReloadRequested -> onFetchData()
        }
    }

    private suspend fun resolveCoachZone(): TimeZone? {
        when (val profile = profileRepository.me()) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = profile) }
                postSideEffect(SeriesResultSideEffect.ShowFailure(profile))
                return null
            }
            is RequestResult.Success -> {
                val zone = profile.data.zoneId?.let(weeks::parseZone)
                if (zone == null) {
                    val failure = RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "",
                        devMessage = "У тренера нет часового пояса: zoneId=${profile.data.zoneId}",
                    )
                    updateState { it.copy(isLoading = false, failure = failure) }
                    postSideEffect(SeriesResultSideEffect.ShowFailure(failure))
                }
                return zone
            }
        }
    }

    private suspend fun showResult(result: SlotSeriesResult, zone: TimeZone) {
        val createdLabels = result.created.map { slot -> formatSlot(startsAt = slot.startsAt, zone = zone) }
        val skippedLabels = result.skipped.map { slot -> formatSlot(startsAt = slot.startsAt, zone = zone) }
        updateState { current ->
            current.copy(
                createdLabels = createdLabels.toImmutableList(),
                skippedLabels = skippedLabels.toImmutableList(),
                isExpired = false,
                failure = null,
                isLoading = false,
            )
        }
    }

    private suspend fun formatSlot(startsAt: Instant, zone: TimeZone): String {
        val dateTime = startsAt.toLocalDateTime(zone)
        return "${formatScheduleDate(dateTime.date)}, ${timeOfDayOf(dateTime)}"
    }
}
