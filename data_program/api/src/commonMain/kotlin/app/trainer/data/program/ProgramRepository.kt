package app.trainer.data.program

import app.trainer.entities.RequestResult
import kotlinx.datetime.LocalDate

sealed interface AssignedProgram {

    data object None : AssignedProgram

    data class Active(val program: ClientProgram) : AssignedProgram
}

interface ProgramRepository {

    suspend fun programs(): RequestResult<List<ProgramSummary>>

    suspend fun create(title: String, weeksCount: Int): RequestResult<TrainingProgram>

    suspend fun program(programId: String): RequestResult<TrainingProgram>

    suspend fun duplicate(programId: String, title: String): RequestResult<TrainingProgram>

    suspend fun saveDay(programId: String, draft: ProgramDayDraft): RequestResult<TrainingProgram>

    suspend fun archive(programId: String): RequestResult<Unit>

    suspend fun assign(
        programId: String,
        clientUserId: String,
        startsOn: LocalDate,
    ): RequestResult<ClientProgram>

    suspend fun endAssignment(clientUserId: String): RequestResult<Unit>

    suspend fun clientProgram(clientUserId: String): RequestResult<AssignedProgram>

    suspend fun ownProgram(): RequestResult<AssignedProgram>

    suspend fun plannedWorkouts(from: LocalDate, to: LocalDate): RequestResult<List<PlannedWorkout>>
}
