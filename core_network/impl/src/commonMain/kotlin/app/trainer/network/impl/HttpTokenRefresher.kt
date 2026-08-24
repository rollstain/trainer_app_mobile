package app.trainer.network.impl

import app.trainer.logger.Logger
import app.trainer.network.AuthTokens
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val LOG_TAG = "token-refresh"

@Serializable
private data class RefreshRequest(
    @SerialName("refreshToken")
    val refreshToken: String,
)

@Serializable
private data class AuthTokensResponse(
    @SerialName("accessToken")
    val accessToken: String?,
    @SerialName("refreshToken")
    val refreshToken: String?,
)

class HttpTokenRefresher(
    private val baseUrl: String,
    private val json: Json,
    private val logger: Logger,
) : TokenRefresher {

    private val plainClient: HttpClient by lazy {
        createPlatformHttpClient {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            defaultRequest { url(baseUrl) }
        }
    }

    override suspend fun refresh(current: AuthTokens): AuthTokens? {
        return try {
            val response: HttpResponse = plainClient.post("auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken = current.refreshToken))
            }
            if (!response.status.isSuccess()) {
                logger.info(tag = LOG_TAG, message = "Обновление отклонено: ${response.status.value}")
                return null
            }
            val refreshed = response.body<AuthTokensResponse>()
            val accessToken = refreshed.accessToken
            val refreshToken = refreshed.refreshToken
            if (accessToken == null || refreshToken == null) {
                logger.error(tag = LOG_TAG, message = "В ответе обновления нет одного из токенов")
                null
            } else {
                AuthTokens(accessToken = accessToken, refreshToken = refreshToken)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            logger.error(tag = LOG_TAG, message = "Обновление не удалось", throwable = failure)
            null
        }
    }
}
