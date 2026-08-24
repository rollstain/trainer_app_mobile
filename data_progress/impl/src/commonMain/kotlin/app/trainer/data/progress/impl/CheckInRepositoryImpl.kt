package app.trainer.data.progress.impl

import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInDraft
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.PreparedPhotoUpload
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.PresignedUploader
import app.trainer.network.safeRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate

class CheckInRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: ProgressMapper,
    private val presignedUploader: PresignedUploader,
) : CheckInRepository {

    private val client get() = httpClientProvider.client

    override suspend fun ownCheckIns(from: LocalDate, to: LocalDate): RequestResult<List<CheckIn>> {
        return checkInsOf {
            client.get("check-ins") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
    }

    override suspend fun clientCheckIns(
        clientUserId: String,
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<CheckIn>> {
        return checkInsOf {
            client.get("coach/clients/$clientUserId/check-ins") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
    }

    override suspend fun save(checkInDate: LocalDate, draft: CheckInDraft): RequestResult<CheckIn> {
        val saved = safeRequest<CheckInResponse> {
            client.put("check-ins/$checkInDate") {
                contentType(ContentType.Application.Json)
                setBody(
                    SaveCheckInRequest(
                        weightGrams = draft.weightGrams,
                        waistMillimeters = draft.waistMillimeters,
                        chestMillimeters = draft.chestMillimeters,
                        hipsMillimeters = draft.hipsMillimeters,
                        wellbeing = draft.wellbeing,
                        sleepQuality = draft.sleepQuality,
                        notes = draft.notes,
                        photoIds = draft.photoIds,
                    )
                )
            }
        }
        return when (saved) {
            is RequestResult.Error -> saved
            is RequestResult.Success -> {
                val checkIn = mapper.toCheckIn(saved.data)
                if (checkIn == null) mappingFailed() else RequestResult.Success(checkIn)
            }
        }
    }

    override suspend fun preparePhotoUpload(
        fileName: String,
        contentType: String,
        sizeBytes: Long,
    ): RequestResult<PreparedPhotoUpload> {
        val prepared = safeRequest<PrepareUploadResponse> {
            client.post("check-ins/photos") {
                contentType(ContentType.Application.Json)
                setBody(
                    PrepareUploadRequest(
                        fileName = fileName,
                        contentType = contentType,
                        sizeBytes = sizeBytes,
                    )
                )
            }
        }
        return when (prepared) {
            is RequestResult.Error -> prepared
            is RequestResult.Success -> {
                val photoId = prepared.data.mediaFileId
                val uploadUrl = prepared.data.uploadUrl
                val downloadUrl = prepared.data.downloadUrl
                if (photoId == null || uploadUrl == null || downloadUrl == null) {
                    RequestResult.Error(
                        statusCode = null,
                        userMessage = "Не удалось подготовить загрузку фото",
                        devMessage = "В ответе нет mediaFileId, uploadUrl или downloadUrl",
                    )
                } else {
                    RequestResult.Success(
                        PreparedPhotoUpload(
                            photoId = photoId,
                            uploadUrl = uploadUrl,
                            downloadUrl = downloadUrl,
                        )
                    )
                }
            }
        }
    }

    override suspend fun uploadPhoto(
        uploadUrl: String,
        contentType: String,
        bytes: ByteArray,
    ): RequestResult<Unit> {
        return presignedUploader.upload(uploadUrl = uploadUrl, contentType = contentType, bytes = bytes)
    }

    override suspend fun deletePhoto(photoId: String): RequestResult<Unit> {
        return safeRequest { client.delete("check-ins/photos/$photoId") }
    }

    private suspend fun checkInsOf(request: suspend () -> HttpResponse): RequestResult<List<CheckIn>> {
        val loaded = safeRequest<List<CheckInResponse>> { request() }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toCheckIn))
        }
    }

    private fun mappingFailed(): RequestResult.Error = RequestResult.Error(
        statusCode = null,
        userMessage = "Не удалось прочитать чек-ин",
        devMessage = "CheckInResponse не разобран",
    )
}
