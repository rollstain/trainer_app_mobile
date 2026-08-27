package app.trainer.data.traininglog.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateExerciseRequest(
    @SerialName("name")
    val name: String,
    @SerialName("muscleGroup")
    val muscleGroup: String?,
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
    @SerialName("muscleGroup")
    val muscleGroup: String?,
    @SerialName("kind")
    val kind: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("videoUrl")
    val videoUrl: String?,
    @SerialName("isOwnedByCoach")
    val isOwnedByCoach: Boolean?,
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
