package app.trainer.data.auth.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RedeemInviteRequest(
    @SerialName("code")
    val code: String,
    @SerialName("displayName")
    val displayName: String,
    @SerialName("deviceInfo")
    val deviceInfo: String,
)

@Serializable
data class AuthTokensResponse(
    @SerialName("accessToken")
    val accessToken: String?,
    @SerialName("refreshToken")
    val refreshToken: String?,
)

@Serializable
data class InviteResponse(
    @SerialName("code")
    val code: String?,
    @SerialName("expiresAt")
    val expiresAt: String?,
)
