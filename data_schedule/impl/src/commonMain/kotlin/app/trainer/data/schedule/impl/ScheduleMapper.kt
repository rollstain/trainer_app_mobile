package app.trainer.data.schedule.impl

import app.trainer.data.schedule.ClientSchedule
import app.trainer.data.schedule.ClientSlot
import app.trainer.data.schedule.CoachSchedule
import app.trainer.data.schedule.CoachSlot
import app.trainer.data.schedule.SkipReason
import app.trainer.data.schedule.SkippedSlot
import app.trainer.data.schedule.SlotChangeKind
import app.trainer.data.schedule.SlotChangeRequest
import app.trainer.data.schedule.SlotChangeStatus
import app.trainer.data.schedule.SlotParticipant
import app.trainer.data.schedule.SlotSeriesResult
import app.trainer.data.schedule.SlotStatus
import app.trainer.logger.Logger
import kotlin.time.Instant

private const val LOG_TAG = "schedule-mapper"

class ScheduleMapper(private val logger: Logger) {

    fun toCoachSchedule(response: CoachScheduleResponse): CoachSchedule? {
        val coachId = response.coachId ?: return skipped(entity = "CoachSchedule", field = "coachId")
        val zoneId = response.zoneId ?: return skipped(entity = "CoachSchedule", field = "zoneId")
        return CoachSchedule(
            coachId = coachId,
            zoneId = zoneId,
            slots = response.slots.orEmpty().mapNotNull(::toCoachSlot),
        )
    }

    fun toClientSchedule(response: ClientScheduleResponse): ClientSchedule? {
        val coachId = response.coachId ?: return skipped(entity = "ClientSchedule", field = "coachId")
        val zoneId = response.zoneId ?: return skipped(entity = "ClientSchedule", field = "zoneId")
        val cancellationWindowHours = response.cancellationWindowHours
            ?: return skipped(entity = "ClientSchedule", field = "cancellationWindowHours")
        return ClientSchedule(
            coachId = coachId,
            zoneId = zoneId,
            cancellationWindowHours = cancellationWindowHours,
            slots = response.slots.orEmpty().mapNotNull(::toClientSlot),
        )
    }

    fun toCoachSlot(response: CoachSlotResponse): CoachSlot? {
        val id = response.id ?: return skipped(entity = "CoachSlot", field = "id")
        val startsAt = parseInstant(response.startsAt) ?: return skipped(entity = "CoachSlot", field = "startsAt")
        val duration = response.durationMinutes ?: return skipped(entity = "CoachSlot", field = "durationMinutes")
        val status = parseSlotStatus(response.status) ?: return skipped(entity = "CoachSlot", field = "status")
        val capacity = response.capacity ?: return skipped(entity = "CoachSlot", field = "capacity")
        val takenSeats = response.takenSeats ?: return skipped(entity = "CoachSlot", field = "takenSeats")
        return CoachSlot(
            id = id,
            startsAt = startsAt,
            durationMinutes = duration,
            status = status,
            clientUserId = response.clientUserId,
            clientDisplayName = response.clientDisplayName,
            pendingChangeRequestId = response.pendingChangeRequestId,
            capacity = capacity,
            takenSeats = takenSeats,
            participants = response.participants.orEmpty().mapNotNull(::toParticipant),
        )
    }

    private fun toParticipant(response: SlotParticipantResponse): SlotParticipant? {
        val userId = response.userId ?: return skipped(entity = "SlotParticipant", field = "userId")
        return SlotParticipant(userId = userId, displayName = response.displayName)
    }

    fun toClientSlot(response: ClientSlotResponse): ClientSlot? {
        val id = response.id ?: return skipped(entity = "ClientSlot", field = "id")
        val startsAt = parseInstant(response.startsAt) ?: return skipped(entity = "ClientSlot", field = "startsAt")
        val duration = response.durationMinutes ?: return skipped(entity = "ClientSlot", field = "durationMinutes")
        val isBookedByMe = response.isBookedByMe ?: return skipped(entity = "ClientSlot", field = "isBookedByMe")
        val isAvailable = response.isAvailable ?: return skipped(entity = "ClientSlot", field = "isAvailable")
        val canRequestChange = response.canRequestChange
            ?: return skipped(entity = "ClientSlot", field = "canRequestChange")
        val isOnWaitlist = response.isOnWaitlist
            ?: return skipped(entity = "ClientSlot", field = "isOnWaitlist")
        val capacity = response.capacity ?: return skipped(entity = "ClientSlot", field = "capacity")
        val takenSeats = response.takenSeats ?: return skipped(entity = "ClientSlot", field = "takenSeats")
        return ClientSlot(
            id = id,
            startsAt = startsAt,
            durationMinutes = duration,
            isBookedByMe = isBookedByMe,
            isAvailable = isAvailable,
            pendingChangeRequestId = response.pendingChangeRequestId,
            canRequestChange = canRequestChange,
            isOnWaitlist = isOnWaitlist,
            capacity = capacity,
            takenSeats = takenSeats,
        )
    }

    fun toSeriesResult(response: CreateSlotSeriesResponse): SlotSeriesResult {
        return SlotSeriesResult(
            created = response.created.orEmpty().mapNotNull(::toCoachSlot),
            skipped = response.skipped.orEmpty().mapNotNull(::toSkippedSlot),
        )
    }

    fun toChangeRequest(response: SlotChangeRequestResponse): SlotChangeRequest? {
        val id = response.id ?: return skipped(entity = "SlotChangeRequest", field = "id")
        val slotId = response.slotId ?: return skipped(entity = "SlotChangeRequest", field = "slotId")
        val slotStartsAt = parseInstant(response.slotStartsAt)
            ?: return skipped(entity = "SlotChangeRequest", field = "slotStartsAt")
        val requestedBy = response.requestedByUserId
            ?: return skipped(entity = "SlotChangeRequest", field = "requestedByUserId")
        val kind = parseChangeKind(response.kind) ?: return skipped(entity = "SlotChangeRequest", field = "kind")
        val status = parseChangeStatus(response.status)
            ?: return skipped(entity = "SlotChangeRequest", field = "status")
        val createdAt = parseInstant(response.createdAt)
            ?: return skipped(entity = "SlotChangeRequest", field = "createdAt")
        return SlotChangeRequest(
            id = id,
            slotId = slotId,
            slotStartsAt = slotStartsAt,
            requestedByUserId = requestedBy,
            requestedByDisplayName = response.requestedByDisplayName,
            kind = kind,
            proposedStartsAt = parseInstant(response.proposedStartsAt),
            status = status,
            createdAt = createdAt,
        )
    }

    private fun toSkippedSlot(response: SkippedSlotResponse): SkippedSlot? {
        val startsAt = parseInstant(response.startsAt) ?: return skipped(entity = "SkippedSlot", field = "startsAt")
        val reason = SkipReason.entries.firstOrNull { it.name == response.reason }
            ?: return skipped(entity = "SkippedSlot", field = "reason")
        return SkippedSlot(startsAt = startsAt, reason = reason)
    }

    private fun parseSlotStatus(raw: String?): SlotStatus? = SlotStatus.entries.firstOrNull { it.name == raw }

    private fun parseChangeKind(raw: String?): SlotChangeKind? =
        SlotChangeKind.entries.firstOrNull { it.name == raw }

    private fun parseChangeStatus(raw: String?): SlotChangeStatus? =
        SlotChangeStatus.entries.firstOrNull { it.name == raw }

    private fun parseInstant(raw: String?): Instant? {
        if (raw == null) return null
        return runCatching { Instant.parse(raw) }.getOrNull()
    }

    private fun <T> skipped(entity: String, field: String): T? {
        logger.error(tag = LOG_TAG, message = "Пропущен $entity: в ответе нет или не разобрано поле $field")
        return null
    }
}
