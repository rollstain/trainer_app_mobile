package app.trainer.feature.schedule.presentation.coach.mvi

import app.trainer.data.schedule.SlotChangeKind
import app.trainer.data.schedule.SlotStatus
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

data class CoachSlotRow(
    val slotId: String,
    val startMinutesOfDay: Int,
    val durationMinutes: Int,
    val timeLabel: String,
    val durationLabel: String,
    val status: SlotStatus,
    val clientDisplayName: String?,
    val hasPendingChangeRequest: Boolean,
    val isGroup: Boolean,
    val hasParticipants: Boolean,
    val seatsLabel: String,
    val capacity: Int,
    val takenSeats: Int,
    val participants: ImmutableList<SlotParticipantRow>,
)

data class ScheduleDay(
    val date: LocalDate,
    val weekdayLabel: String,
    val dayNumberLabel: String,
    val isToday: Boolean,
    val isDayOff: Boolean,
    val slots: ImmutableList<CoachSlotRow>,
)

data class ChangeRequestRow(
    val requestId: String,
    val slotTimeLabel: String,
    val proposedTimeLabel: String?,
    val kind: SlotChangeKind,
    val requestedByDisplayName: String?,
)

enum class SlotActionsKind { Booked, Free }

data class SlotParticipantRow(
    val clientUserId: String,
    val displayName: String,
)

data class ClientPickRow(
    val clientUserId: String,
    val displayName: String,
)

data class ClientPicker(
    val clients: ImmutableList<ClientPickRow>,
    val isLoading: Boolean,
)

data class SlotActions(
    val slotId: String,
    val title: String,
    val kind: SlotActionsKind,
    val isResolving: Boolean,
    val hasFreeSeats: Boolean,
    val participants: ImmutableList<SlotParticipantRow>,
    val picker: ClientPicker?,
)

data class CoachScheduleState(
    val weekStart: LocalDate?,
    val weekTitle: String,
    val days: ImmutableList<ScheduleDay>,
    val pendingRequests: ImmutableList<ChangeRequestRow>,
    val nextSlotId: String?,
    val slotActions: SlotActions?,
    val selectedDate: LocalDate?,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val selectedDay: ScheduleDay?
        get() = days.firstOrNull { it.date == selectedDate }

    companion object {

        fun initial(): CoachScheduleState = CoachScheduleState(
            weekStart = null,
            weekTitle = "",
            days = persistentListOf(),
            pendingRequests = persistentListOf(),
            nextSlotId = null,
            slotActions = null,
            selectedDate = null,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface CoachScheduleEvent {

    data object OnRetryClicked : CoachScheduleEvent

    data object OnPreviousWeekClicked : CoachScheduleEvent

    data object OnNextWeekClicked : CoachScheduleEvent

    data class OnDaySelected(val date: LocalDate) : CoachScheduleEvent

    data class OnCreateSlotClicked(val date: LocalDate?) : CoachScheduleEvent

    data object OnSlotCreated : CoachScheduleEvent

    data class OnSlotClicked(val slotId: String) : CoachScheduleEvent

    data object OnSlotActionsDismissed : CoachScheduleEvent

    data object OnAssignClientClicked : CoachScheduleEvent

    data class OnClientPicked(val clientUserId: String) : CoachScheduleEvent

    data object OnClientPickerDismissed : CoachScheduleEvent

    data class OnParticipantRemoved(val clientUserId: String) : CoachScheduleEvent

    data class OnCancelSlotClicked(val slotId: String) : CoachScheduleEvent

    data class OnCompleteSlotClicked(val slotId: String) : CoachScheduleEvent

    data class OnChangeRequestResolved(val requestId: String, val approve: Boolean) : CoachScheduleEvent
}

sealed interface CoachScheduleSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : CoachScheduleSideEffect

    data class OpenSlotCreation(val dateIso: String?) : CoachScheduleSideEffect

    data class OpenGroupSession(val slotId: String) : CoachScheduleSideEffect
}
