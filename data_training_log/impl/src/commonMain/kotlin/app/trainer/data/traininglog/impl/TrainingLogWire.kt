package app.trainer.data.traininglog.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateExerciseRequest(
    @SerialName("name")
    val name: String,
    @SerialName("primaryMuscle")
    val primaryMuscle: String,
    @SerialName("equipment")
    val equipment: String,
    @SerialName("kind")
    val kind: String,
    @SerialName("description")
    val description: String?,
    @SerialName("videoUrl")
    val videoUrl: String?,
)

@Serializable
data class TrainingSetRequest(
    @SerialName("exerciseId")
    val exerciseId: String,
    @SerialName("repetitions")
    val repetitions: Int?,
    @SerialName("weightGrams")
    val weightGrams: Int?,
    @SerialName("durationSeconds")
    val durationSeconds: Int?,
    @SerialName("distanceMeters")
    val distanceMeters: Int?,
)

@Serializable
data class SaveTrainingLogRequest(
    @SerialName("slotId")
    val slotId: String?,
    @SerialName("notes")
    val notes: String?,
    @SerialName("sets")
    val sets: List<TrainingSetRequest>,
)

@Serializable
data class ExerciseResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("name")
    val name: String?,
    @SerialName("primaryMuscle")
    val primaryMuscle: String?,
    @SerialName("equipment")
    val equipment: String?,
    @SerialName("kind")
    val kind: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("videoUrl")
    val videoUrl: String?,
    @SerialName("video")
    val video: ExerciseVideoResponse?,
    @SerialName("ownerKind")
    val ownerKind: String?,
    @SerialName("ownerDisplayName")
    val ownerDisplayName: String?,
    @SerialName("lastRepetitions")
    val lastRepetitions: Int?,
    @SerialName("lastWeightGrams")
    val lastWeightGrams: Int?,
    @SerialName("lastDurationSeconds")
    val lastDurationSeconds: Int?,
    @SerialName("lastDistanceMeters")
    val lastDistanceMeters: Int?,
)

@Serializable
data class TrainingSetResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("exerciseId")
    val exerciseId: String?,
    @SerialName("exerciseName")
    val exerciseName: String?,
    @SerialName("kind")
    val kind: String?,
    @SerialName("position")
    val position: Int?,
    @SerialName("repetitions")
    val repetitions: Int?,
    @SerialName("weightGrams")
    val weightGrams: Int?,
    @SerialName("durationSeconds")
    val durationSeconds: Int?,
    @SerialName("distanceMeters")
    val distanceMeters: Int?,
    @SerialName("isPersonalRecord")
    val isPersonalRecord: Boolean?,
)

@Serializable
data class TrainingLogEntryResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("clientUserId")
    val clientUserId: String?,
    @SerialName("entryDate")
    val entryDate: String?,
    @SerialName("slotId")
    val slotId: String?,
    @SerialName("notes")
    val notes: String?,
    @SerialName("sets")
    val sets: List<TrainingSetResponse>?,
    @SerialName("totalVolumeGrams")
    val totalVolumeGrams: Long?,
)

@Serializable
data class DiaryDayResponse(
    @SerialName("entryDate")
    val entryDate: String?,
    @SerialName("volumeGrams")
    val volumeGrams: Long?,
)

@Serializable
data class ClientDiarySummaryResponse(
    @SerialName("clientUserId")
    val clientUserId: String?,
    @SerialName("displayName")
    val displayName: String?,
    @SerialName("linkedAt")
    val linkedAt: String?,
    @SerialName("lastEntryDate")
    val lastEntryDate: String?,
    @SerialName("days")
    val days: List<DiaryDayResponse>?,
)

@Serializable
data class ExerciseVideoResponse(
    @SerialName("downloadUrl")
    val downloadUrl: String?,
)

@Serializable
data class PrepareVideoUploadRequest(
    @SerialName("fileName")
    val fileName: String,
    @SerialName("contentType")
    val contentType: String,
    @SerialName("sizeBytes")
    val sizeBytes: Long,
)

@Serializable
data class PrepareVideoUploadResponse(
    @SerialName("mediaFileId")
    val mediaFileId: String?,
    @SerialName("uploadUrl")
    val uploadUrl: String?,
)

@Serializable
data class AttachExerciseVideoRequest(
    @SerialName("mediaFileId")
    val mediaFileId: String,
)
