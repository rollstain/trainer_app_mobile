package app.trainer.data.traininglog

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

enum class ExerciseKind { STRENGTH, CARDIO, BODYWEIGHT }

data class LastPerformed(
    val repetitions: Int?,
    val weightGrams: Int?,
    val durationSeconds: Int?,
    val distanceMeters: Int?,
)

enum class MuscleGroup {
    CHEST, LATS, MIDDLE_BACK, LOWER_BACK, TRAPS, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    ABDOMINALS, QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, ADDUCTORS, ABDUCTORS, NECK,
}

enum class Equipment {
    BARBELL, DUMBBELL, EZ_BAR, KETTLEBELL, MACHINE, CABLE, BODYWEIGHT, BANDS, BALL, OTHER,
}

enum class ExerciseOwnerKind { SHARED, COACH, CLIENT }

data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscle: MuscleGroup?,
    val equipment: Equipment?,
    val kind: ExerciseKind,
    val ownerKind: ExerciseOwnerKind,
    val ownerDisplayName: String?,
    val description: String?,
    val videoUrl: String?,
    val videoFileUrl: String?,
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

data class DiaryDay(
    val entryDate: LocalDate,
    val volumeGrams: Long,
)

data class ClientDiarySummary(
    val clientUserId: String,
    val displayName: String,
    val linkedAt: Instant?,
    val lastEntryDate: LocalDate?,
    val days: List<DiaryDay>,
)
