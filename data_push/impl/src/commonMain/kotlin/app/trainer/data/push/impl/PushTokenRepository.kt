package app.trainer.data.push.impl

import app.trainer.data.push.PushPlatform
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.safeRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterPushTokenRequest(
    @SerialName("token")
    val token: String,
    @SerialName("platform")
    val platform: String,
    @SerialName("locale")
    val locale: String,
)

class PushTokenRepository(private val httpClientProvider: HttpClientProvider) {

    suspend fun register(token: String, platform: PushPlatform, language: String): RequestResult<Unit> {
        return safeRequest<Unit> {
            httpClientProvider.client.post("me/push-tokens") {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterPushTokenRequest(
                        token = token,
                        platform = platform.name,
                        locale = language,
                    )
                )
            }
        }
    }
}
