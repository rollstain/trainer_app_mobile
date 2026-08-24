package app.trainer.network.impl

import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.PresignedUploader
import app.trainer.network.safeRequest
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PlainPresignedUploader(
    private val httpClientProvider: HttpClientProvider,
) : PresignedUploader {

    override suspend fun upload(
        uploadUrl: String,
        contentType: String,
        bytes: ByteArray,
    ): RequestResult<Unit> {
        return safeRequest {
            httpClientProvider.plainClient.put(uploadUrl) {
                contentType(ContentType.parse(contentType))
                setBody(bytes)
            }
        }
    }
}
