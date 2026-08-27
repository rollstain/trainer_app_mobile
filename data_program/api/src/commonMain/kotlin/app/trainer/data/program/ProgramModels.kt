package app.trainer.data.program

import kotlinx.datetime.LocalDate

data class ProgramSummary(
    val id: String,
    val title: String,
    val weeksCount: Int,
    val filledDaysCount: Int,
    val assignedClientsCount: Int,
)

data class ProgramExerciseLine(
    val exerciseId: String,
    val exerciseName: String,
    val setsCount: Int,
    val repetitions: Int?,
    val weightGrams: Int?,
    val restSeconds: Int?,
    val note: String?,
)

data class ProgramDay(
    val weekNumber: Int,
    val dayOfWeek: Int,
    val title: String,
    val exercises: List<ProgramExerciseLine>,
)

data class TrainingProgram(
    val id: String,
    val title: String,
    val weeksCount: Int,
    val days: List<ProgramDay>,
)

data class PlannedWorkout(
    val date: LocalDate,
    val programTitle: String,
    val dayTitle: String,
    val weekNumber: Int,
    val exercises: List<ProgramExerciseLine>,
)

data class ClientProgram(
    val programId: String,
    val programTitle: String,
    val startsOn: LocalDate,
)

data class ProgramExerciseDraft(
    val exerciseId: String,
    val setsCount: Int,
    val repetitions: Int?,
    val weightGrams: Int?,
    val restSeconds: Int?,
    val note: String?,
)

data class ProgramDayDraft(
    val weekNumber: Int,
    val dayOfWeek: Int,
    val title: String,
    val exercises: List<ProgramExerciseDraft>,
)
