package app.trainer.data.profile.impl

import app.trainer.data.profile.ProfileRepository
import app.trainer.data.profile.UserProfile
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.HttpClientProvider
import app.trainer.network.safeRequest
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val LOG_TAG = "profile"

@Serializable
data class UpdateContactRequest(
    @SerialName("phone")
    val phone: String?,
    @SerialName("email")
    val email: String?,
)

@Serializable
data class MeResponse(
    @SerialName("userId")
    val userId: String?,
    @SerialName("displayName")
    val displayName: String?,
    @SerialName("phone")
    val phone: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("coachId")
    val coachId: String?,
    @SerialName("zoneId")
    val zoneId: String?,
)

class ProfileRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val logger: Logger,
) : ProfileRepository {

    override suspend fun updateContact(phone: String?, email: String?): RequestResult<UserProfile> {
        val updated = safeRequest<MeResponse> {
            httpClientProvider.client.patch("me/contact") {
                contentType(ContentType.Application.Json)
                setBody(UpdateContactRequest(phone = phone, email = email))
            }
        }
        return when (updated) {
            is RequestResult.Error -> updated
            is RequestResult.Success -> toProfile(updated.data)
        }
    }

    override suspend fun me(): RequestResult<UserProfile> {
        val loaded = safeRequest<MeResponse> {
            httpClientProvider.client.get("me")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> toProfile(loaded.data)
        }
    }

    private fun toProfile(response: MeResponse): RequestResult<UserProfile> {
        val userId = response.userId
        val displayName = response.displayName
        if (userId == null || displayName == null) {
            logger.error(tag = LOG_TAG, message = "В ответе /me нет userId или displayName")
            return RequestResult.Error(
                statusCode = null,
                userMessage = "",
                devMessage = "Ответ /me не удалось разобрать в UserProfile",
            )
        }
        return RequestResult.Success(
            UserProfile(
                userId = userId,
                displayName = displayName,
                phone = response.phone,
                email = response.email,
                coachId = response.coachId,
                zoneId = response.zoneId,
            )
        )
    }
}
