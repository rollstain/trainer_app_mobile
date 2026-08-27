package app.trainer.data.auth.impl

import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.InviteCode
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.AuthTokens
import app.trainer.network.HttpClientProvider
import app.trainer.network.SessionEvents
import app.trainer.network.TokenStorage
import app.trainer.network.safeRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val LOG_TAG = "auth"

class AuthRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val tokenStorage: TokenStorage,
    private val sessionEvents: SessionEvents,
    private val logger: Logger,
) : AuthRepository {

    override suspend fun isAuthorized(): Boolean = tokenStorage.read() != null

    override suspend fun redeemInvite(
        code: String,
        displayName: String,
        deviceInfo: String,
    ): RequestResult<Unit> {
        val redeemed = safeRequest<AuthTokensResponse> {
            httpClientProvider.client.post("auth/invites/redeem") {
                contentType(ContentType.Application.Json)
                setBody(
                    RedeemInviteRequest(
                        code = code.trim().uppercase(),
                        displayName = displayName.trim(),
                        deviceInfo = deviceInfo,
                    )
                )
            }
        }
        return when (redeemed) {
            is RequestResult.Error -> redeemed
            is RequestResult.Success -> storeTokens(redeemed.data)
        }
    }

    override suspend fun createInvite(): RequestResult<InviteCode> {
        val created = safeRequest<InviteResponse> {
            httpClientProvider.client.post("auth/invites")
        }
        return when (created) {
            is RequestResult.Error -> created
            is RequestResult.Success -> {
                val code = created.data.code
                if (code == null) {
                    logger.error(tag = LOG_TAG, message = "В ответе на создание приглашения нет кода")
                    RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "",
                        devMessage = "Ответ auth/invites не содержит кода",
                    )
                } else {
                    RequestResult.Success(
                        InviteCode(code = code, expiresAtLabel = created.data.expiresAt.orEmpty())
                    )
                }
            }
        }
    }

    override suspend fun logout() {
        tokenStorage.clear()
        httpClientProvider.forgetAuthenticatedUser()
        sessionEvents.notifyAuthChanged()
    }

    private suspend fun storeTokens(response: AuthTokensResponse): RequestResult<Unit> {
        val accessToken = response.accessToken
        val refreshToken = response.refreshToken
        if (accessToken == null || refreshToken == null) {
            logger.error(tag = LOG_TAG, message = "В ответе на приглашение нет одного из токенов")
            return RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "",
                devMessage = "Ответ auth/invites/redeem не содержит пары токенов",
            )
        }
        tokenStorage.write(AuthTokens(accessToken = accessToken, refreshToken = refreshToken))
        httpClientProvider.forgetAuthenticatedUser()
        sessionEvents.notifyAuthChanged()
        return RequestResult.Success(Unit)
    }
}
