package app.trainer.data.traininglog

import kotlinx.datetime.LocalDate

enum class ExerciseKind { STRENGTH, CARDIO, BODYWEIGHT }

data class LastPerformed(
    val repetitions: Int?,
    val weightGrams: Int?,
    val durationSeconds: Int?,
    val distanceMeters: Int?,
)

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: String?,
    val kind: ExerciseKind,
    val isOwnedByCoach: Boolean,
    val description: String?,
    val videoUrl: String?,
    val lastPerformed: LastPerformed?,
)

data class TrainingSet(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val kind: ExerciseKind,
    val position: Int,
    val repetitions: Int?,
    val weightGrams: Int?,
    val durationSeconds: Int?,
    val distanceMeters: Int?,
    val isPersonalRecord: Boolean,
)

data class TrainingLogEntry(
    val id: String,
    val clientUserId: String,
    val entryDate: LocalDate,
    val slotId: String?,
    val notes: String?,
    val sets: List<TrainingSet>,
    val totalVolumeGrams: Long,
)

data class TrainingSetDraft(
    val exerciseId: String,
    val repetitions: Int?,
    val weightGrams: Int?,
    val durationSeconds: Int?,
    val distanceMeters: Int?,
)

data class TrainingLogDraft(
    val slotId: String?,
    val notes: String?,
    val sets: List<TrainingSetDraft>,
)
