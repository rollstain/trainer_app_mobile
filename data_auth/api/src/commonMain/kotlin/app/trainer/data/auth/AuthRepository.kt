package app.trainer.data.auth

import app.trainer.entities.RequestResult

data class InviteCode(
    val code: String,
    val expiresAtLabel: String,
)

interface AuthRepository {

    suspend fun isAuthorized(): Boolean

    suspend fun redeemInvite(code: String, displayName: String, deviceInfo: String): RequestResult<Unit>

    suspend fun createInvite(): RequestResult<InviteCode>

    suspend fun logout()
}
