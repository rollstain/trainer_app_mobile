package app.trainer.data.program.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProgramSummaryResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("title")
    val title: String?,
    @SerialName("weeksCount")
    val weeksCount: Int?,
    @SerialName("filledDaysCount")
    val filledDaysCount: Int?,
    @SerialName("assignedClientsCount")
    val assignedClientsCount: Int?,
)

@Serializable
data class ProgramExerciseResponse(
    @SerialName("exerciseId")
    val exerciseId: String?,
    @SerialName("exerciseName")
    val exerciseName: String?,
    @SerialName("position")
    val position: Int?,
    @SerialName("setsCount")
    val setsCount: Int?,
    @SerialName("repetitions")
    val repetitions: Int?,
    @SerialName("weightGrams")
    val weightGrams: Int?,
    @SerialName("restSeconds")
    val restSeconds: Int?,
    @SerialName("note")
    val note: String?,
)

@Serializable
data class ProgramDayResponse(
    @SerialName("weekNumber")
    val weekNumber: Int?,
    @SerialName("dayOfWeek")
    val dayOfWeek: Int?,
    @SerialName("title")
    val title: String?,
    @SerialName("exercises")
    val exercises: List<ProgramExerciseResponse>?,
)

@Serializable
data class ProgramResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("title")
    val title: String?,
    @SerialName("weeksCount")
    val weeksCount: Int?,
    @SerialName("days")
    val days: List<ProgramDayResponse>?,
)

@Serializable
data class PlannedWorkoutResponse(
    @SerialName("date")
    val date: String?,
    @SerialName("programTitle")
    val programTitle: String?,
    @SerialName("dayTitle")
    val dayTitle: String?,
    @SerialName("weekNumber")
    val weekNumber: Int?,
    @SerialName("exercises")
    val exercises: List<ProgramExerciseResponse>?,
)

@Serializable
data class ClientProgramResponse(
    @SerialName("programId")
    val programId: String?,
    @SerialName("programTitle")
    val programTitle: String?,
    @SerialName("startsOn")
    val startsOn: String?,
)

@Serializable
data class ClientProgramStateResponse(
    @SerialName("program")
    val program: ClientProgramResponse?,
)

@Serializable
data class CreateProgramRequest(
    @SerialName("title")
    val title: String,
    @SerialName("weeksCount")
    val weeksCount: Int,
)

@Serializable
data class DuplicateProgramRequest(
    @SerialName("title")
    val title: String,
)

@Serializable
data class ProgramExerciseRequest(
    @SerialName("exerciseId")
    val exerciseId: String,
    @SerialName("setsCount")
    val setsCount: Int,
    @SerialName("repetitions")
    val repetitions: Int?,
    @SerialName("weightGrams")
    val weightGrams: Int?,
    @SerialName("restSeconds")
    val restSeconds: Int?,
    @SerialName("note")
    val note: String?,
)

@Serializable
data class SaveProgramDayRequest(
    @SerialName("weekNumber")
    val weekNumber: Int,
    @SerialName("dayOfWeek")
    val dayOfWeek: Int,
    @SerialName("title")
    val title: String,
    @SerialName("exercises")
    val exercises: List<ProgramExerciseRequest>,
)

@Serializable
data class AssignProgramRequest(
    @SerialName("clientUserId")
    val clientUserId: String,
    @SerialName("startsOn")
    val startsOn: String,
)
