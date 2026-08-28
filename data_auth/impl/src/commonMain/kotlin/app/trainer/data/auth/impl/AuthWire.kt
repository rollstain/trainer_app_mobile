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

@Serializable
data class ExternalSignInRequest(
    @SerialName("provider")
    val provider: String,
    @SerialName("token")
    val token: String,
    @SerialName("deviceInfo")
    val deviceInfo: String,
)

@Serializable
data class LinkIdentityRequest(
    @SerialName("provider")
    val provider: String,
    @SerialName("token")
    val token: String,
)

@Serializable
data class JoinCoachRequest(
    @SerialName("code")
    val code: String,
)

@Serializable
data class LinkedIdentityResponse(
    @SerialName("provider")
    val provider: String?,
    @SerialName("linkedAt")
    val linkedAt: String?,
)

@Serializable
data class DeviceSessionResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("deviceInfo")
    val deviceInfo: String?,
    @SerialName("lastSeenAt")
    val lastSeenAt: String?,
    @SerialName("isCurrent")
    val isCurrent: Boolean?,
)
