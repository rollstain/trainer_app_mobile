package app.trainer.feature.progress.presentation.progress.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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

data class ProgressState(
    val checkInDateLabel: String,
    val checkInSummary: String,
    val hasCheckIn: Boolean,
    val habits: ImmutableList<HabitRow>,
    val newHabitTitle: String,
    val isLoading: Boolean,
    val isFailed: Boolean,
) {

    val isAddHabitEnabled: Boolean
        get() = newHabitTitle.isNotBlank()

    companion object {

        fun initial(): ProgressState = ProgressState(
            checkInDateLabel = "",
            checkInSummary = "",
            hasCheckIn = false,
            habits = persistentListOf(),
            newHabitTitle = "",
            isLoading = true,
            isFailed = false,
        )
    }
}

sealed interface ProgressEvent {

    data object OnRetryClicked : ProgressEvent

    data object OnCheckInClicked : ProgressEvent

    data object OnHabitAdded : ProgressEvent

    data class OnNewHabitTitleChanged(val title: String) : ProgressEvent

    data class OnHabitDayToggled(val habitId: String, val dateIso: String) : ProgressEvent

    data class OnHabitArchived(val habitId: String) : ProgressEvent
}

sealed interface ProgressSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : ProgressSideEffect

    data class OpenCheckIn(val dateIso: String) : ProgressSideEffect
}
