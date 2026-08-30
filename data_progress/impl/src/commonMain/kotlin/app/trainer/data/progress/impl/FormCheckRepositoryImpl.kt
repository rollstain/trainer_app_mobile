package app.trainer.data.progress.impl

import app.trainer.data.progress.FormCheck
import app.trainer.data.progress.FormCheckRepository
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
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val LOG_TAG = "form-check-repository"

class FormCheckRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: ProgressMapper,
    private val presignedUploader: PresignedUploader,
    private val logger: Logger,
) : FormCheckRepository {

    private val client get() = httpClientProvider.client

    override suspend fun ownFormChecks(limit: Int, after: String?): RequestResult<Paged<List<FormCheck>>> {
        return formChecksPageOf {
            client.get("me/form-checks") {
                parameter("limit", limit)
                after?.let { parameter("after", it) }
            }
        }
    }

    override suspend fun awaitingReview(limit: Int, after: String?): RequestResult<Paged<List<FormCheck>>> {
        return formChecksPageOf {
            client.get("coach/form-checks/awaiting") {
                parameter("limit", limit)
                after?.let { parameter("after", it) }
            }
        }
    }

    override suspend fun submit(
        fileName: String,
        contentType: String,
        bytes: ByteArray,
        exerciseId: String?,
        note: String?,
    ): RequestResult<FormCheck> {
        val prepared = safeRequest<PrepareFormCheckUploadResponse> {
            client.post("form-checks/uploads") {
                contentType(ContentType.Application.Json)
                setBody(
                    PrepareFormCheckUploadRequest(
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
            logger.error(tag = LOG_TAG, message = "В ответе нет ссылки на загрузку разбора")
            return RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "Не удалось начать загрузку видео",
                devMessage = "PrepareFormCheckUploadResponse без mediaFileId или uploadUrl",
            )
        }

        val uploaded = presignedUploader.upload(uploadUrl = uploadUrl, contentType = contentType, bytes = bytes)
        if (uploaded is RequestResult.Error) return uploaded

        return formCheckOf {
            client.post("form-checks") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateFormCheckRequest(
                        mediaFileId = mediaFileId,
                        exerciseId = exerciseId,
                        note = note,
                    )
                )
            }
        }
    }

    override suspend fun review(formCheckId: String, comment: String?): RequestResult<FormCheck> {
        return formCheckOf {
            client.post("coach/form-checks/$formCheckId/review") {
                contentType(ContentType.Application.Json)
                setBody(ReviewFormCheckRequest(comment = comment))
            }
        }
    }

    private suspend fun formChecksPageOf(
        request: suspend () -> HttpResponse,
    ): RequestResult<Paged<List<FormCheck>>> {
        val loaded = safePagedRequest<List<FormCheckResponse>> { request() }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(
                Paged(
                    items = loaded.data.items.mapNotNull(mapper::toFormCheck),
                    nextCursor = loaded.data.nextCursor,
                )
            )
        }
    }

    private suspend fun formCheckOf(request: suspend () -> HttpResponse): RequestResult<FormCheck> {
        val loaded = safeRequest<FormCheckResponse> { request() }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                val formCheck = mapper.toFormCheck(loaded.data)
                if (formCheck == null) {
                    RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "Не удалось прочитать разбор",
                        devMessage = "FormCheckResponse без обязательных полей",
                    )
                } else {
                    RequestResult.Success(formCheck)
                }
            }
        }
    }
}
