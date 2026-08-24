package app.trainer.network

import app.trainer.entities.RequestResult

interface PresignedUploader {

    suspend fun upload(uploadUrl: String, contentType: String, bytes: ByteArray): RequestResult<Unit>
}
