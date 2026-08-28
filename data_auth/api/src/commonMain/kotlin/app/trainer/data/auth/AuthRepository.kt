package app.trainer.data.auth

import app.trainer.entities.RequestResult

data class InviteCode(
    val code: String,
    val expiresAtLabel: String,
)

data class InvitePreview(
    val coachDisplayName: String,
    val needsDisplayName: Boolean,
)

enum class AuthProvider { YANDEX, VK, APPLE, GOOGLE }

data class LinkedIdentity(
    val provider: AuthProvider,
    val linkedAtIso: String,
)

data class DeviceSession(
    val id: String,
    val deviceInfo: String,
    val lastSeenAtIso: String,
    val isCurrent: Boolean,
)

interface AuthRepository {

    suspend fun isAuthorized(): Boolean

    suspend fun previewInvite(code: String): RequestResult<InvitePreview>

    suspend fun redeemInvite(code: String, displayName: String, deviceInfo: String): RequestResult<Unit>

    suspend fun signInWithProvider(
        provider: AuthProvider,
        token: String,
        deviceInfo: String,
    ): RequestResult<Unit>

    suspend fun joinCoach(code: String): RequestResult<Unit>

    suspend fun linkedIdentities(): RequestResult<List<LinkedIdentity>>

    suspend fun linkProvider(provider: AuthProvider, token: String): RequestResult<List<LinkedIdentity>>

    suspend fun unlinkProvider(provider: AuthProvider): RequestResult<List<LinkedIdentity>>

    suspend fun sessions(): RequestResult<List<DeviceSession>>

    suspend fun revokeSession(sessionId: String): RequestResult<Unit>

    suspend fun revokeOtherSessions(): RequestResult<Unit>

    suspend fun createInvite(): RequestResult<InviteCode>

    suspend fun logout()
}
