package app.trainer.data.program.impl

import app.trainer.data.program.AssignedProgram
import app.trainer.data.program.ClientProgram
import app.trainer.data.program.PlannedWorkout
import app.trainer.data.program.ProgramDayDraft
import app.trainer.data.program.ProgramRepository
import app.trainer.data.program.ProgramSummary
import app.trainer.data.program.TrainingProgram
import app.trainer.entities.Paged
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.safePagedRequest
import app.trainer.network.safeRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate

class ProgramRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: ProgramMapper,
) : ProgramRepository {

    override suspend fun programs(limit: Int, after: String?): RequestResult<Paged<List<ProgramSummary>>> {
        val loaded = safePagedRequest<List<ProgramSummaryResponse>> {
            httpClientProvider.client.get("coach/programs") {
                parameter("limit", limit)
                after?.let { parameter("after", it) }
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(
                Paged(
                    items = loaded.data.items.mapNotNull(mapper::toSummary),
                    nextCursor = loaded.data.nextCursor,
                )
            )
        }
    }

    override suspend fun create(title: String, weeksCount: Int): RequestResult<TrainingProgram> {
        return programOf(
            safeRequest {
                httpClientProvider.client.post("coach/programs") {
                    contentType(ContentType.Application.Json)
                    setBody(CreateProgramRequest(title = title, weeksCount = weeksCount))
                }
            }
        )
    }

    override suspend fun program(programId: String): RequestResult<TrainingProgram> {
        return programOf(safeRequest { httpClientProvider.client.get("coach/programs/$programId") })
    }

    override suspend fun duplicate(programId: String, title: String): RequestResult<TrainingProgram> {
        return programOf(
            safeRequest {
                httpClientProvider.client.post("coach/programs/$programId/duplicate") {
                    contentType(ContentType.Application.Json)
                    setBody(DuplicateProgramRequest(title = title))
                }
            }
        )
    }

    override suspend fun saveDay(programId: String, draft: ProgramDayDraft): RequestResult<TrainingProgram> {
        return programOf(
            safeRequest {
                httpClientProvider.client.put("coach/programs/$programId/days") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        SaveProgramDayRequest(
                            weekNumber = draft.weekNumber,
                            dayOfWeek = draft.dayOfWeek,
                            title = draft.title,
                            exercises = draft.exercises.map { line ->
                                ProgramExerciseRequest(
                                    exerciseId = line.exerciseId,
                                    setsCount = line.setsCount,
                                    repetitions = line.repetitions,
                                    weightGrams = line.weightGrams,
                                    restSeconds = line.restSeconds,
                                    note = line.note,
                                )
                            },
                        )
                    )
                }
            }
        )
    }

    override suspend fun archive(programId: String): RequestResult<Unit> {
        return safeRequest { httpClientProvider.client.delete("coach/programs/$programId") }
    }

    override suspend fun assign(
        programId: String,
        clientUserId: String,
        startsOn: LocalDate,
    ): RequestResult<ClientProgram> {
        val assigned = safeRequest<ClientProgramResponse> {
            httpClientProvider.client.post("coach/programs/$programId/assign") {
                contentType(ContentType.Application.Json)
                setBody(
                    AssignProgramRequest(
                        clientUserId = clientUserId,
                        startsOn = startsOn.toString(),
                    )
                )
            }
        }
        return when (assigned) {
            is RequestResult.Error -> assigned
            is RequestResult.Success -> mapper.toClientProgram(assigned.data)
                ?.let { RequestResult.Success(it) }
                ?: unreadable(entity = "ClientProgram")
        }
    }

    override suspend fun endAssignment(clientUserId: String): RequestResult<Unit> {
        return safeRequest { httpClientProvider.client.delete("coach/clients/$clientUserId/program") }
    }

    override suspend fun clientProgram(clientUserId: String): RequestResult<AssignedProgram> {
        return assignedOf(safeRequest { httpClientProvider.client.get("coach/clients/$clientUserId/program") })
    }

    override suspend fun ownProgram(): RequestResult<AssignedProgram> {
        return assignedOf(safeRequest { httpClientProvider.client.get("me/program") })
    }

    override suspend fun plannedWorkouts(
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<PlannedWorkout>> {
        val loaded = safeRequest<List<PlannedWorkoutResponse>> {
            httpClientProvider.client.get("me/program/planned") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toPlannedWorkout))
        }
    }

    private fun programOf(response: RequestResult<ProgramResponse>): RequestResult<TrainingProgram> {
        return when (response) {
            is RequestResult.Error -> response
            is RequestResult.Success -> mapper.toProgram(response.data)
                ?.let { RequestResult.Success(it) }
                ?: unreadable(entity = "TrainingProgram")
        }
    }

    private fun assignedOf(
        response: RequestResult<ClientProgramStateResponse>,
    ): RequestResult<AssignedProgram> {
        return when (response) {
            is RequestResult.Error -> response
            is RequestResult.Success -> {
                val program = response.data.program?.let(mapper::toClientProgram)
                RequestResult.Success(
                    program?.let(AssignedProgram::Active) ?: AssignedProgram.None
                )
            }
        }
    }

    private fun unreadable(entity: String): RequestResult.Error = RequestResult.Error(
        kind = RequestFailure.Parsing,
        statusCode = null,
        userMessage = "",
        devMessage = "$entity не читается из ответа",
    )
}
