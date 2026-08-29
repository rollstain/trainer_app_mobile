package app.trainer.data.profile.impl

import app.trainer.data.profile.ProfileRepository
import app.trainer.data.profile.UserProfile
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.HttpClientProvider
import app.trainer.network.SessionEvents
import app.trainer.network.safeRequest
import com.russhwolf.settings.Settings
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val LOG_TAG = "profile"
private const val IS_COACH_KEY = "profile.isCoach"

@Serializable
data class UpdateContactRequest(
    @SerialName("phone")
    val phone: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("currentPassword")
    val currentPassword: String?,
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
    @SerialName("login")
    val login: String?,
    @SerialName("hasPassword")
    val hasPassword: Boolean?,
    @SerialName("passwordUpdatedAt")
    val passwordUpdatedAt: String?,
    @SerialName("coachId")
    val coachId: String?,
    @SerialName("zoneId")
    val zoneId: String?,
    @SerialName("hasCoach")
    val hasCoach: Boolean?,
)

@Serializable
data class BecomeCoachRequest(
    @SerialName("displayName")
    val displayName: String,
    @SerialName("zoneId")
    val zoneId: String,
)

class ProfileRepositoryImpl(
    private val settings: Settings,
    private val ioDispatcher: CoroutineDispatcher,
    private val httpClientProvider: HttpClientProvider,
    private val sessionEvents: SessionEvents,
    private val logger: Logger,
) : ProfileRepository {

    override suspend fun updateContact(
        phone: String?,
        email: String?,
        currentPassword: String?,
    ): RequestResult<UserProfile> {
        val updated = safeRequest<MeResponse> {
            httpClientProvider.client.patch("me/contact") {
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateContactRequest(
                        phone = phone,
                        email = email,
                        currentPassword = currentPassword,
                    )
                )
            }
        }
        return when (updated) {
            is RequestResult.Error -> updated
            is RequestResult.Success -> toProfile(updated.data)
        }
    }

    override suspend fun becomeCoach(displayName: String, zoneId: String): RequestResult<UserProfile> {
        val created = safeRequest<MeResponse> {
            httpClientProvider.client.post("me/coach") {
                contentType(ContentType.Application.Json)
                setBody(BecomeCoachRequest(displayName = displayName, zoneId = zoneId))
            }
        }
        return when (created) {
            is RequestResult.Error -> created
            is RequestResult.Success -> toProfile(created.data)
                .also(::rememberRole)
                .also { sessionEvents.notifyProfileChanged() }
        }
    }

    override suspend fun me(): RequestResult<UserProfile> {
        val loaded = safeRequest<MeResponse> {
            httpClientProvider.client.get("me")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> toProfile(loaded.data).also(::rememberRole)
        }
    }

    override suspend fun lastKnownIsCoach(): Boolean? = withContext(ioDispatcher) {
        if (settings.hasKey(IS_COACH_KEY)) settings.getBoolean(IS_COACH_KEY, false) else null
    }

    override suspend fun clearLocalData() {
        withContext(ioDispatcher) { settings.remove(IS_COACH_KEY) }
    }

    private fun rememberRole(profile: RequestResult<UserProfile>) {
        if (profile !is RequestResult.Success) return
        settings.putBoolean(IS_COACH_KEY, profile.data.coachId != null)
    }

    private fun toProfile(response: MeResponse): RequestResult<UserProfile> {
        val userId = response.userId
        val displayName = response.displayName
        val hasCoach = response.hasCoach
        val hasPassword = response.hasPassword
        val isComplete = userId != null && displayName != null && hasCoach != null && hasPassword != null
        if (!isComplete) {
            logger.error(tag = LOG_TAG, message = "В ответе /me не хватает полей профиля")
            return RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "",
                devMessage = "Ответ /me не удалось разобрать в UserProfile",
            )
        }
        return RequestResult.Success(
            UserProfile(
                userId = requireNotNull(userId),
                displayName = requireNotNull(displayName),
                phone = response.phone,
                email = response.email,
                login = response.login,
                hasPassword = requireNotNull(hasPassword),
                passwordUpdatedAtIso = response.passwordUpdatedAt,
                coachId = response.coachId,
                zoneId = response.zoneId,
                hasCoach = requireNotNull(hasCoach),
            )
        )
    }
}
