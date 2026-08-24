package app.trainer.data.schedule.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSlotRequest(
    @SerialName("startsAt")
    val startsAt: String,
    @SerialName("durationMinutes")
    val durationMinutes: Int,
)

@Serializable
data class CreateSlotSeriesRequest(
    @SerialName("startDate")
    val startDate: String,
    @SerialName("weeksCount")
    val weeksCount: Int,
    @SerialName("daysOfWeek")
    val daysOfWeek: List<String>,
    @SerialName("timeOfDay")
    val timeOfDay: String,
    @SerialName("durationMinutes")
    val durationMinutes: Int,
)

@Serializable
data class AssignSlotRequest(
    @SerialName("clientUserId")
    val clientUserId: String,
)

@Serializable
data class SlotChangeRequestBody(
    @SerialName("kind")
    val kind: String,
    @SerialName("proposedStartsAt")
    val proposedStartsAt: String?,
)

@Serializable
data class ResolveChangeRequestBody(
    @SerialName("approve")
    val approve: Boolean,
)

@Serializable
data class CoachSlotResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("startsAt")
    val startsAt: String?,
    @SerialName("durationMinutes")
    val durationMinutes: Int?,
    @SerialName("status")
    val status: String?,
    @SerialName("clientUserId")
    val clientUserId: String?,
    @SerialName("clientDisplayName")
    val clientDisplayName: String?,
    @SerialName("pendingChangeRequestId")
    val pendingChangeRequestId: String?,
)

@Serializable
data class ClientSlotResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("startsAt")
    val startsAt: String?,
    @SerialName("durationMinutes")
    val durationMinutes: Int?,
    @SerialName("isBookedByMe")
    val isBookedByMe: Boolean?,
    @SerialName("isAvailable")
    val isAvailable: Boolean?,
    @SerialName("pendingChangeRequestId")
    val pendingChangeRequestId: String?,
    @SerialName("canRequestChange")
    val canRequestChange: Boolean?,
    @SerialName("isOnWaitlist")
    val isOnWaitlist: Boolean?,
)

@Serializable
data class CoachScheduleResponse(
    @SerialName("coachId")
    val coachId: String?,
    @SerialName("zoneId")
    val zoneId: String?,
    @SerialName("slots")
    val slots: List<CoachSlotResponse>?,
)

@Serializable
data class ClientScheduleResponse(
    @SerialName("coachId")
    val coachId: String?,
    @SerialName("zoneId")
    val zoneId: String?,
    @SerialName("cancellationWindowHours")
    val cancellationWindowHours: Int?,
    @SerialName("slots")
    val slots: List<ClientSlotResponse>?,
)

@Serializable
data class SkippedSlotResponse(
    @SerialName("startsAt")
    val startsAt: String?,
    @SerialName("reason")
    val reason: String?,
)

@Serializable
data class CreateSlotSeriesResponse(
    @SerialName("created")
    val created: List<CoachSlotResponse>?,
    @SerialName("skipped")
    val skipped: List<SkippedSlotResponse>?,
)

@Serializable
data class SlotChangeRequestResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("slotId")
    val slotId: String?,
    @SerialName("slotStartsAt")
    val slotStartsAt: String?,
    @SerialName("requestedByUserId")
    val requestedByUserId: String?,
    @SerialName("requestedByDisplayName")
    val requestedByDisplayName: String?,
    @SerialName("kind")
    val kind: String?,
    @SerialName("proposedStartsAt")
    val proposedStartsAt: String?,
    @SerialName("status")
    val status: String?,
    @SerialName("createdAt")
    val createdAt: String?,
)
