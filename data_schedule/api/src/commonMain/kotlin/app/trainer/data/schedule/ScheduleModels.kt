package app.trainer.data.schedule

import kotlin.time.Instant

enum class SlotStatus { FREE, BOOKED, CANCELLED, COMPLETED }

enum class SlotChangeKind { RESCHEDULE, CANCEL }

enum class SlotChangeStatus { PENDING, APPROVED, REJECTED }

enum class SkipReason { OVERLAPS_EXISTING_SLOT }

data class SlotParticipant(
    val userId: String,
    val displayName: String?,
)

data class CoachSlot(
    val id: String,
    val startsAt: Instant,
    val durationMinutes: Int,
    val status: SlotStatus,
    val clientUserId: String?,
    val clientDisplayName: String?,
    val pendingChangeRequestId: String?,
    val capacity: Int,
    val takenSeats: Int,
    val participants: List<SlotParticipant>,
) {

    val isGroup: Boolean get() = capacity > 1
}

data class ClientSlot(
    val id: String,
    val startsAt: Instant,
    val durationMinutes: Int,
    val isBookedByMe: Boolean,
    val isAvailable: Boolean,
    val pendingChangeRequestId: String?,
    val canRequestChange: Boolean,
    val isOnWaitlist: Boolean,
    val capacity: Int,
    val takenSeats: Int,
) {

    val isGroup: Boolean get() = capacity > 1

    val freeSeats: Int get() = (capacity - takenSeats).coerceAtLeast(0)
}

data class CoachSchedule(
    val coachId: String,
    val zoneId: String,
    val slots: List<CoachSlot>,
)

data class ClientSchedule(
    val coachId: String,
    val zoneId: String,
    val cancellationWindowHours: Int,
    val slots: List<ClientSlot>,
)

data class SkippedSlot(
    val startsAt: Instant,
    val reason: SkipReason,
)

data class SlotSeriesResult(
    val created: List<CoachSlot>,
    val skipped: List<SkippedSlot>,
)

data class SlotChangeRequest(
    val id: String,
    val slotId: String,
    val slotStartsAt: Instant,
    val requestedByUserId: String,
    val requestedByDisplayName: String?,
    val kind: SlotChangeKind,
    val proposedStartsAt: Instant?,
    val status: SlotChangeStatus,
    val createdAt: Instant,
)
