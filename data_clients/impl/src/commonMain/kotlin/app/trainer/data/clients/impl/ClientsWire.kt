package app.trainer.data.clients.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientNoteRequest(
    @SerialName("kind")
    val kind: String,
    @SerialName("title")
    val title: String,
    @SerialName("details")
    val details: String?,
    @SerialName("isPinned")
    val isPinned: Boolean,
)

@Serializable
data class ClientNoteResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("clientUserId")
    val clientUserId: String?,
    @SerialName("kind")
    val kind: String?,
    @SerialName("title")
    val title: String?,
    @SerialName("details")
    val details: String?,
    @SerialName("isPinned")
    val isPinned: Boolean?,
    @SerialName("createdAt")
    val createdAt: String?,
    @SerialName("updatedAt")
    val updatedAt: String?,
)

@Serializable
data class CoachSummaryResponse(
    @SerialName("coachId")
    val coachId: String?,
    @SerialName("userId")
    val userId: String?,
    @SerialName("displayName")
    val displayName: String?,
    @SerialName("zoneId")
    val zoneId: String?,
    @SerialName("cancellationWindowHours")
    val cancellationWindowHours: Int?,
)

@Serializable
data class CoachPolicyResponse(
    @SerialName("cancellationWindowHours")
    val cancellationWindowHours: Int?,
)

@Serializable
data class UpdateCoachPolicyRequest(
    @SerialName("cancellationWindowHours")
    val cancellationWindowHours: Int,
)

@Serializable
data class CoachClientResponse(
    @SerialName("userId")
    val userId: String?,
    @SerialName("displayName")
    val displayName: String?,
    @SerialName("status")
    val status: String?,
)
