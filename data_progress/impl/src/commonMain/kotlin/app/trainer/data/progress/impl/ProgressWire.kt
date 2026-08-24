package app.trainer.data.progress.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaveCheckInRequest(
    @SerialName("weightGrams")
    val weightGrams: Int?,
    @SerialName("waistMillimeters")
    val waistMillimeters: Int?,
    @SerialName("chestMillimeters")
    val chestMillimeters: Int?,
    @SerialName("hipsMillimeters")
    val hipsMillimeters: Int?,
    @SerialName("wellbeing")
    val wellbeing: Int?,
    @SerialName("sleepQuality")
    val sleepQuality: Int?,
    @SerialName("notes")
    val notes: String?,
    @SerialName("photoIds")
    val photoIds: List<String>,
)

@Serializable
data class MediaFileResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("contentType")
    val contentType: String?,
    @SerialName("sizeBytes")
    val sizeBytes: Long?,
    @SerialName("originalName")
    val originalName: String?,
    @SerialName("downloadUrl")
    val downloadUrl: String?,
)

@Serializable
data class PrepareUploadRequest(
    @SerialName("fileName")
    val fileName: String,
    @SerialName("contentType")
    val contentType: String,
    @SerialName("sizeBytes")
    val sizeBytes: Long,
)

@Serializable
data class PrepareUploadResponse(
    @SerialName("mediaFileId")
    val mediaFileId: String?,
    @SerialName("uploadUrl")
    val uploadUrl: String?,
    @SerialName("downloadUrl")
    val downloadUrl: String?,
)

@Serializable
data class CheckInResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("clientUserId")
    val clientUserId: String?,
    @SerialName("checkInDate")
    val checkInDate: String?,
    @SerialName("weightGrams")
    val weightGrams: Int?,
    @SerialName("waistMillimeters")
    val waistMillimeters: Int?,
    @SerialName("chestMillimeters")
    val chestMillimeters: Int?,
    @SerialName("hipsMillimeters")
    val hipsMillimeters: Int?,
    @SerialName("wellbeing")
    val wellbeing: Int?,
    @SerialName("sleepQuality")
    val sleepQuality: Int?,
    @SerialName("notes")
    val notes: String?,
    @SerialName("photos")
    val photos: List<MediaFileResponse>?,
)

@Serializable
data class CreateHabitRequest(
    @SerialName("title")
    val title: String,
)

@Serializable
data class HabitResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("clientUserId")
    val clientUserId: String?,
    @SerialName("title")
    val title: String?,
    @SerialName("isSetByCoach")
    val isSetByCoach: Boolean?,
    @SerialName("doneDates")
    val doneDates: List<String>?,
)
