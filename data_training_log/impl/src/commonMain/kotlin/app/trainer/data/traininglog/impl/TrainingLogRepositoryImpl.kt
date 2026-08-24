package app.trainer.data.traininglog.impl

import app.trainer.data.traininglog.Exercise
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.TrainingLogDraft
import app.trainer.data.traininglog.TrainingLogEntry
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.safeRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate

class TrainingLogRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: TrainingLogMapper,
) : TrainingLogRepository {

    private val client get() = httpClientProvider.client

    override suspend fun availableExercises(): RequestResult<List<Exercise>> {
        val loaded = safeRequest<List<ExerciseResponse>> {
            client.get("exercises")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toExercise))
        }
    }

    override suspend fun createExercise(
        name: String,
        muscleGroup: String?,
        kind: ExerciseKind,
    ): RequestResult<Exercise> {
        val created = safeRequest<ExerciseResponse> {
            client.post("coach/exercises") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateExerciseRequest(
                        name = name,
                        muscleGroup = muscleGroup,
                        kind = kind.name,
                    )
                )
            }
        }
        return when (created) {
            is RequestResult.Error -> created
            is RequestResult.Success -> {
                val exercise = mapper.toExercise(created.data)
                if (exercise == null) mappingFailed("Exercise") else RequestResult.Success(exercise)
            }
        }
    }

    override suspend fun ownEntries(from: LocalDate, to: LocalDate): RequestResult<List<TrainingLogEntry>> {
        val loaded = safeRequest<List<TrainingLogEntryResponse>> {
            client.get("me/training-log") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
        return toEntries(loaded)
    }

    override suspend fun clientEntries(
        clientUserId: String,
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<TrainingLogEntry>> {
        val loaded = safeRequest<List<TrainingLogEntryResponse>> {
            client.get("coach/clients/$clientUserId/training-log") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
        return toEntries(loaded)
    }

    override suspend fun saveEntry(
        entryDate: LocalDate,
        draft: TrainingLogDraft,
    ): RequestResult<TrainingLogEntry> {
        val saved = safeRequest<TrainingLogEntryResponse> {
            client.put("me/training-log/$entryDate") {
                contentType(ContentType.Application.Json)
                setBody(
                    SaveTrainingLogRequest(
                        slotId = draft.slotId,
                        notes = draft.notes,
                        sets = draft.sets.map { set ->
                            TrainingSetRequest(
                                exerciseId = set.exerciseId,
                                repetitions = set.repetitions,
                                weightGrams = set.weightGrams,
                                durationSeconds = set.durationSeconds,
                                distanceMeters = set.distanceMeters,
                            )
                        },
                    )
                )
            }
        }
        return when (saved) {
            is RequestResult.Error -> saved
            is RequestResult.Success -> {
                val entry = mapper.toEntry(saved.data)
                if (entry == null) mappingFailed("TrainingLogEntry") else RequestResult.Success(entry)
            }
        }
    }

    private fun toEntries(
        result: RequestResult<List<TrainingLogEntryResponse>>,
    ): RequestResult<List<TrainingLogEntry>> {
        return when (result) {
            is RequestResult.Error -> result
            is RequestResult.Success -> RequestResult.Success(result.data.mapNotNull(mapper::toEntry))
        }
    }

    private fun mappingFailed(entity: String): RequestResult.Error = RequestResult.Error(
        statusCode = null,
        userMessage = "",
        devMessage = "Ответ сервера не удалось разобрать в $entity",
    )
}
