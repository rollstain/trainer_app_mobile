package app.trainer.data.auth.impl

import app.trainer.data.auth.DeviceSession
import app.trainer.data.auth.DeviceSessionsRepository
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.HttpClientProvider
import app.trainer.network.safeRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post

private const val LOG_TAG = "device-sessions"

class DeviceSessionsRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val logger: Logger,
) : DeviceSessionsRepository {

    override suspend fun sessions(): RequestResult<List<DeviceSession>> {
        val loaded = safeRequest<List<DeviceSessionResponse>> { httpClientProvider.client.get("auth/sessions") }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(::toSession))
        }
    }

    override suspend fun revokeSession(sessionId: String): RequestResult<Unit> {
        val revoked = safeRequest<Unit> { httpClientProvider.client.delete("auth/sessions/$sessionId") }
        return when (revoked) {
            is RequestResult.Error -> revoked
            is RequestResult.Success -> RequestResult.Success(Unit)
        }
    }

    override suspend fun revokeOtherSessions(): RequestResult<Unit> {
        val revoked = safeRequest<Unit> { httpClientProvider.client.post("auth/sessions/revoke-others") }
        return when (revoked) {
            is RequestResult.Error -> revoked
            is RequestResult.Success -> RequestResult.Success(Unit)
        }
    }

    private fun toSession(response: DeviceSessionResponse): DeviceSession? {
        val id = response.id ?: return skippedSession()
        val deviceInfo = response.deviceInfo ?: return skippedSession()
        val lastSeenAt = response.lastSeenAt ?: return skippedSession()
        val isCurrent = response.isCurrent ?: return skippedSession()
        return DeviceSession(
            id = id,
            deviceInfo = deviceInfo,
            lastSeenAtIso = lastSeenAt,
            isCurrent = isCurrent,
        )
    }

    private fun skippedSession(): DeviceSession? {
        logger.error(tag = LOG_TAG, message = "Пропущена сессия без обязательных полей")
        return null
    }
}
