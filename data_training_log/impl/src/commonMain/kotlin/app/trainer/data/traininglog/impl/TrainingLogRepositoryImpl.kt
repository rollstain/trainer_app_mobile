package app.trainer.data.traininglog.impl

import app.trainer.data.traininglog.ClientDiarySummary
import app.trainer.data.traininglog.Equipment
import app.trainer.data.traininglog.Exercise
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.MuscleGroup
import app.trainer.data.traininglog.SaveOutcome
import app.trainer.data.traininglog.TrainingLogDraft
import app.trainer.data.traininglog.TrainingLogEntry
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.Paged
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.HttpClientProvider
import app.trainer.network.PresignedUploader
import app.trainer.network.safePagedRequest
import app.trainer.network.safeRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

private const val LOG_TAG = "training-log"

class TrainingLogRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: TrainingLogMapper,
    private val outbox: TrainingLogOutbox,
    private val presignedUploader: PresignedUploader,
    private val logger: Logger,
) : TrainingLogRepository {

    private val client get() = httpClientProvider.client

    override suspend fun availableExercises(limit: Int?, after: String?): RequestResult<Paged<List<Exercise>>> {
        val loaded = safePagedRequest<List<ExerciseResponse>> {
            client.get("exercises") {
                limit?.let { parameter("limit", it) }
                after?.let { parameter("after", it) }
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(
                Paged(
                    items = loaded.data.items.mapNotNull(mapper::toExercise),
                    nextCursor = loaded.data.nextCursor,
                )
            )
        }
    }

    override suspend fun coachDiarySummary(
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<ClientDiarySummary>> {
        val loaded = safeRequest<List<ClientDiarySummaryResponse>> {
            client.get("coach/clients/diary-summary") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toDiarySummary))
        }
    }

    override suspend fun uploadExerciseVideo(
        exerciseId: String,
        fileName: String,
        contentType: String,
        bytes: ByteArray,
    ): RequestResult<Exercise> {
        val prepared = safeRequest<PrepareVideoUploadResponse> {
            client.post("coach/exercises/video-uploads") {
                contentType(ContentType.Application.Json)
                setBody(
                    PrepareVideoUploadRequest(
                        fileName = fileName,
                        contentType = contentType,
                        sizeBytes = bytes.size.toLong(),
                    )
                )
            }
        }
        if (prepared is RequestResult.Error) return prepared
        val ready = (prepared as RequestResult.Success).data
        val mediaFileId = ready.mediaFileId
        val uploadUrl = ready.uploadUrl
        if (mediaFileId == null || uploadUrl == null) {
            logger.error(tag = LOG_TAG, message = "В ответе нет ссылки на загрузку видео")
            return RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "Не удалось начать загрузку видео",
                devMessage = "PrepareVideoUploadResponse без mediaFileId или uploadUrl",
            )
        }

        val uploaded = presignedUploader.upload(uploadUrl = uploadUrl, contentType = contentType, bytes = bytes)
        if (uploaded is RequestResult.Error) return uploaded

        val attached = safeRequest<ExerciseResponse> {
            client.put("coach/exercises/$exerciseId/video") {
                contentType(ContentType.Application.Json)
                setBody(AttachExerciseVideoRequest(mediaFileId = mediaFileId))
            }
        }
        return when (attached) {
            is RequestResult.Error -> attached
            is RequestResult.Success -> {
                val exercise = mapper.toExercise(attached.data)
                if (exercise == null) {
                    RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "Видео загружено, но упражнение не прочиталось",
                        devMessage = "ExerciseResponse без обязательных полей",
                    )
                } else {
                    RequestResult.Success(exercise)
                }
            }
        }
    }

    override suspend fun createExercise(
        name: String,
        primaryMuscle: MuscleGroup,
        equipment: Equipment,
        kind: ExerciseKind,
        description: String?,
        videoUrl: String?,
    ): RequestResult<Exercise> {
        val created = safeRequest<ExerciseResponse> {
            client.post("exercises") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateExerciseRequest(
                        name = name,
                        primaryMuscle = primaryMuscle.name,
                        equipment = equipment.name,
                        kind = kind.name,
                        description = description,
                        videoUrl = videoUrl,
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
    ): RequestResult<SaveOutcome> {
        val request = toRequest(draft)
        outbox.enqueue(entryDate = entryDate, request = request)
        return when (val saved = send(entryDate = entryDate, request = request)) {
            is RequestResult.Success -> {
                outbox.remove(entryDate)
                RequestResult.Success(SaveOutcome.Sent(saved.data))
            }
            is RequestResult.Error -> when (saved.kind) {
                RequestFailure.Network -> RequestResult.Success(SaveOutcome.Queued)
                else -> {
                    outbox.remove(entryDate)
                    saved
                }
            }
        }
    }

    override suspend fun sendQueuedEntries() {
        outbox.queued().forEach { (entryDate, request) ->
            when (val sent = send(entryDate = entryDate, request = request)) {
                is RequestResult.Success -> outbox.remove(entryDate)
                is RequestResult.Error -> when (sent.kind) {
                    RequestFailure.Network -> return
                    else -> {
                        logger.error(
                            tag = LOG_TAG,
                            message = "Запись за $entryDate отклонена сервером, убрана из очереди: ${sent.devMessage}",
                        )
                        outbox.remove(entryDate)
                    }
                }
            }
        }
    }

    override fun observeQueuedDates(): Flow<Set<LocalDate>> = outbox.observeQueuedDates()

    override suspend fun clearLocalData() {
        outbox.clear()
    }

    private suspend fun send(
        entryDate: LocalDate,
        request: SaveTrainingLogRequest,
    ): RequestResult<TrainingLogEntry> {
        val saved = safeRequest<TrainingLogEntryResponse> {
            client.put("me/training-log/$entryDate") {
                contentType(ContentType.Application.Json)
                setBody(request)
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

    private fun toRequest(draft: TrainingLogDraft): SaveTrainingLogRequest = SaveTrainingLogRequest(
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

    private fun toEntries(
        result: RequestResult<List<TrainingLogEntryResponse>>,
    ): RequestResult<List<TrainingLogEntry>> {
        return when (result) {
            is RequestResult.Error -> result
            is RequestResult.Success -> RequestResult.Success(result.data.mapNotNull(mapper::toEntry))
        }
    }

    private fun mappingFailed(entity: String): RequestResult.Error = RequestResult.Error(
        kind = RequestFailure.Parsing,
        statusCode = null,
        userMessage = "",
        devMessage = "Ответ сервера не удалось разобрать в $entity",
    )
}
