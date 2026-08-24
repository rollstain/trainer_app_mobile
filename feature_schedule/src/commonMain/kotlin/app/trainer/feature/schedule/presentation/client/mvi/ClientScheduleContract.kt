package app.trainer.feature.schedule.presentation.client.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class CoachOption(
    val coachId: String,
    val displayName: String,
)

data class ClientSlotRow(
    val slotId: String,
    val timeLabel: String,
    val durationLabel: String,
    val isBookedByMe: Boolean,
    val isAvailable: Boolean,
    val hasPendingChangeRequest: Boolean,
    val canRequestChange: Boolean,
    val isOnWaitlist: Boolean,
    val note: String,
)

data class ClientScheduleDay(
    val date: LocalDate,
    val weekdayLabel: String,
    val dayNumberLabel: String,
    val isSelected: Boolean,
    val isToday: Boolean,
    val isWeekend: Boolean,
    val slots: ImmutableList<ClientSlotRow>,
)

data class ClientScheduleState(
    val coaches: ImmutableList<CoachOption>,
    val selectedCoachId: String?,
    val weekStart: LocalDate?,
    val weekTitle: String,
    val selectedDate: LocalDate?,
    val days: ImmutableList<ClientScheduleDay>,
    val slotPendingCancel: String?,
    val isLoading: Boolean,
    val isFailed: Boolean,
) {

    companion object {

        fun initial(): ClientScheduleState = ClientScheduleState(
            coaches = persistentListOf(),
            selectedCoachId = null,
            weekStart = null,
            weekTitle = "",
            selectedDate = null,
            days = persistentListOf(),
            slotPendingCancel = null,
            isLoading = true,
            isFailed = false,
        )
    }
}

sealed interface ClientScheduleEvent {

    data object OnRetryClicked : ClientScheduleEvent

    data object OnPreviousWeekClicked : ClientScheduleEvent

    data object OnNextWeekClicked : ClientScheduleEvent

    data class OnCoachSelected(val coachId: String) : ClientScheduleEvent

    data class OnDateSelected(val date: LocalDate) : ClientScheduleEvent

    data class OnBookClicked(val slotId: String) : ClientScheduleEvent

    data class OnCancelClicked(val slotId: String) : ClientScheduleEvent

    data object OnCancelConfirmed : ClientScheduleEvent

    data object OnCancelDismissed : ClientScheduleEvent

    data class OnWaitlistToggled(val slotId: String) : ClientScheduleEvent

    data class OnRescheduleRequested(val slotId: String, val proposedStartsAt: Instant) : ClientScheduleEvent
}

sealed interface ClientScheduleSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : ClientScheduleSideEffect

    data object ShowChangeRequestSent : ClientScheduleSideEffect

    data object ShowSlotBooked : ClientScheduleSideEffect
}
