package app.trainer.network

import app.trainer.entities.RequestResult
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiErrorResponse(
    @SerialName("status")
    val status: Int?,
    @SerialName("message")
    val message: String?,
    @SerialName("fieldErrors")
    val fieldErrors: Map<String, String>?,
)

@PublishedApi
internal val apiErrorJson: Json = Json { ignoreUnknownKeys = true }

@PublishedApi
internal fun parseApiError(statusCode: Int, rawBody: String): RequestResult.Error {
    val parsed = runCatching { apiErrorJson.decodeFromString<ApiErrorResponse>(rawBody) }.getOrNull()
    return RequestResult.Error(
        statusCode = statusCode,
        userMessage = parsed?.message.orEmpty(),
        devMessage = rawBody,
    )
}

suspend inline fun <reified T> safeRequest(request: () -> HttpResponse): RequestResult<T> {
    return try {
        val response = request()
        if (response.status.isSuccess()) {
            RequestResult.Success(response.body<T>())
        } else {
            parseApiError(statusCode = response.status.value, rawBody = response.bodyAsText())
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        RequestResult.Error(
            statusCode = null,
            userMessage = "",
            devMessage = failure.message.orEmpty(),
        )
    }
}
