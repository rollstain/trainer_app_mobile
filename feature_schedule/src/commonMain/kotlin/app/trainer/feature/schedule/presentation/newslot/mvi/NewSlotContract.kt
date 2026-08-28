package app.trainer.feature.schedule.presentation.newslot.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

enum class SlotMode { Single, Series }

data class WeekDayToggle(
    val dayOfWeek: DayOfWeek,
    val label: String,
    val isSelected: Boolean,
)

data class NewSlotState(
    val mode: SlotMode,
    val date: LocalDate,
    val dateLabel: String,
    val timeText: String,
    val durationMinutes: Int,
    val capacity: Int,
    val weeksCount: Int,
    val weekDays: ImmutableList<WeekDayToggle>,
    val summaryLabel: String,
    val isSubmitting: Boolean,
) {

    val isSubmitEnabled: Boolean
        get() = timeText.length == TIME_LENGTH &&
            !isSubmitting &&
            (mode == SlotMode.Single || weekDays.any { it.isSelected })

    companion object {

        const val TIME_LENGTH = 5

        fun initial(date: LocalDate): NewSlotState = NewSlotState(
            mode = SlotMode.Single,
            date = date,
            dateLabel = "",
            timeText = "",
            durationMinutes = DEFAULT_DURATION_MINUTES,
            capacity = PERSONAL_CAPACITY,
            weeksCount = DEFAULT_WEEKS,
            weekDays = persistentListOf(),
            summaryLabel = "",
            isSubmitting = false,
        )

        private const val DEFAULT_DURATION_MINUTES = 60
        private const val DEFAULT_WEEKS = 4
        private const val PERSONAL_CAPACITY = 1
    }
}

sealed interface NewSlotEvent {

    data object OnSubmitClicked : NewSlotEvent

    data class OnModeChanged(val mode: SlotMode) : NewSlotEvent

    data class OnTimeChanged(val text: String) : NewSlotEvent

    data class OnDurationChanged(val minutes: Int) : NewSlotEvent

    data class OnCapacityChanged(val capacity: Int) : NewSlotEvent

    data class OnWeeksCountChanged(val weeks: Int) : NewSlotEvent

    data class OnWeekDayToggled(val dayOfWeek: DayOfWeek) : NewSlotEvent
}

sealed interface NewSlotSideEffect {

    data object SlotCreated : NewSlotSideEffect

    data class SeriesCreated(val batchId: String) : NewSlotSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : NewSlotSideEffect
}
