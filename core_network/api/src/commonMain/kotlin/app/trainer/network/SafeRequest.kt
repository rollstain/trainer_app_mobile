package app.trainer.network

import app.trainer.entities.Paged
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

const val NEXT_CURSOR_HEADER = "X-Next-Cursor"

private const val STATUS_BAD_REQUEST = 400
private const val STATUS_UNAUTHORIZED = 401
private const val STATUS_FORBIDDEN = 403
private const val STATUS_NOT_FOUND = 404
private const val STATUS_CONFLICT = 409
private const val STATUS_GONE = 410
private const val STATUS_UNPROCESSABLE_ENTITY = 422
private const val STATUS_SERVER_ERROR_FROM = 500

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
internal fun failureOf(statusCode: Int): RequestFailure = when {
    statusCode == STATUS_UNAUTHORIZED -> RequestFailure.Unauthorized
    statusCode == STATUS_FORBIDDEN -> RequestFailure.Forbidden
    statusCode == STATUS_NOT_FOUND -> RequestFailure.NotFound
    statusCode == STATUS_CONFLICT -> RequestFailure.Conflict
    statusCode == STATUS_GONE -> RequestFailure.Gone
    statusCode == STATUS_BAD_REQUEST -> RequestFailure.Validation
    statusCode == STATUS_UNPROCESSABLE_ENTITY -> RequestFailure.Validation
    statusCode >= STATUS_SERVER_ERROR_FROM -> RequestFailure.Server
    else -> RequestFailure.Unknown
}

@PublishedApi
internal fun parseApiError(statusCode: Int, rawBody: String): RequestResult.Error {
    val parsed = runCatching { apiErrorJson.decodeFromString<ApiErrorResponse>(rawBody) }.getOrNull()
    return RequestResult.Error(
        kind = failureOf(statusCode),
        statusCode = statusCode,
        userMessage = parsed?.message.orEmpty(),
        devMessage = rawBody,
    )
}

@PublishedApi
internal fun transportFailure(failure: Exception): RequestResult.Error {
    val kind = if (failure is SerializationException) {
        RequestFailure.Parsing
    } else {
        RequestFailure.Network
    }
    return RequestResult.Error(
        kind = kind,
        statusCode = null,
        userMessage = "",
        devMessage = failure.message.orEmpty(),
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
        transportFailure(failure)
    }
}

suspend inline fun <reified T> safePagedRequest(request: () -> HttpResponse): RequestResult<Paged<T>> {
    return try {
        val response = request()
        if (response.status.isSuccess()) {
            RequestResult.Success(
                Paged(
                    items = response.body<T>(),
                    nextCursor = response.headers[NEXT_CURSOR_HEADER]?.takeIf { it.isNotBlank() },
                )
            )
        } else {
            parseApiError(statusCode = response.status.value, rawBody = response.bodyAsText())
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        transportFailure(failure)
    }
}
