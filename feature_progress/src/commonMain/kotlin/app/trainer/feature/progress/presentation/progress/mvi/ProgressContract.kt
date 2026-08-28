package app.trainer.feature.progress.presentation.progress.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class ProgressMetric { Weight, Waist, Chest, Hips, Wellbeing, Sleep }

data class MetricChart(
    val metric: ProgressMetric,
    val title: String,
    val values: ImmutableList<Float>,
    val maxLabel: String,
    val minLabel: String,
    val rangeLabel: String,
    val latestLabel: String,
    val deltaLabel: String,
)

data class HabitDay(
    val dateIso: String,
    val weekdayLabel: String,
    val isDone: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

data class HabitRow(
    val habitId: String,
    val title: String,
    val isSetByCoach: Boolean,
    val doneCountLabel: String,
    val days: ImmutableList<HabitDay>,
)

sealed interface CoachReply {

    data object None : CoachReply

    data class Text(val value: String) : CoachReply
}

data class ProgressPhotoRow(
    val photoId: String,
    val url: String,
)

data class ProgressState(
    val checkInDateLabel: String,
    val checkInSummary: String,
    val hasCheckIn: Boolean,
    val coachReply: CoachReply,
    val charts: ImmutableList<MetricChart>,
    val selectedMetric: ProgressMetric?,
    val habits: ImmutableList<HabitRow>,
    val photos: ImmutableList<ProgressPhotoRow>,
    val newHabitTitle: String,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val isAddHabitEnabled: Boolean
        get() = newHabitTitle.isNotBlank()

    val selectedChart: MetricChart?
        get() = charts.firstOrNull { it.metric == selectedMetric }

    companion object {

        fun initial(): ProgressState = ProgressState(
            checkInDateLabel = "",
            checkInSummary = "",
            hasCheckIn = false,
            coachReply = CoachReply.None,
            charts = persistentListOf(),
            selectedMetric = null,
            habits = persistentListOf(),
            photos = persistentListOf(),
            newHabitTitle = "",
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface ProgressEvent {

    data object OnReloadRequested : ProgressEvent

    data object OnCheckInClicked : ProgressEvent

    data object OnComparePhotosClicked : ProgressEvent

    data class OnMetricSelected(val metric: ProgressMetric) : ProgressEvent

    data object OnHabitAdded : ProgressEvent

    data class OnNewHabitTitleChanged(val title: String) : ProgressEvent

    data class OnHabitDayToggled(val habitId: String, val dateIso: String) : ProgressEvent

    data class OnHabitArchived(val habitId: String) : ProgressEvent
}

sealed interface ProgressSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : ProgressSideEffect

    data class OpenCheckIn(val dateIso: String) : ProgressSideEffect

    data object OpenPhotoCompare : ProgressSideEffect
}
