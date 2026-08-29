package app.trainer.data.auth.impl

import app.trainer.data.auth.AuthProvider
import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.InviteCode
import app.trainer.data.auth.InvitePreview
import app.trainer.data.auth.TelegramLoginStart
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.AuthTokens
import app.trainer.network.HttpClientProvider
import app.trainer.network.SessionEvents
import app.trainer.network.TokenStorage
import app.trainer.network.safeRequest
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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

    override suspend fun previewInvite(code: String): RequestResult<InvitePreview> {
        val loaded = safeRequest<InvitePreviewResponse> {
            httpClientProvider.client.get("auth/invites/${code.trim().uppercase()}")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> previewOf(loaded.data)
        }
    }

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

    override suspend fun signUpWithPassword(
        displayName: String,
        email: String,
        login: String?,
        password: String,
        deviceInfo: String,
    ): RequestResult<Unit> {
        return authorizeBy {
            httpClientProvider.client.post("auth/password/sign-up") {
                contentType(ContentType.Application.Json)
                setBody(
                    PasswordSignUpRequest(
                        displayName = displayName.trim(),
                        email = email.trim(),
                        login = login?.trim()?.ifEmpty { null },
                        password = password,
                        deviceInfo = deviceInfo,
                    )
                )
            }
        }
    }

    override suspend fun signInWithPassword(
        identifier: String,
        password: String,
        deviceInfo: String,
    ): RequestResult<Unit> {
        return authorizeBy {
            httpClientProvider.client.post("auth/password/sign-in") {
                contentType(ContentType.Application.Json)
                setBody(
                    PasswordSignInRequest(
                        identifier = identifier.trim(),
                        password = password,
                        deviceInfo = deviceInfo,
                    )
                )
            }
        }
    }

    override suspend fun resetPasswordByTelegram(
        claimToken: String,
        password: String,
        deviceInfo: String,
    ): RequestResult<Unit> {
        return authorizeBy {
            httpClientProvider.client.post("auth/password/reset/telegram") {
                contentType(ContentType.Application.Json)
                setBody(
                    PasswordResetRequest(
                        claimToken = claimToken,
                        password = password,
                        deviceInfo = deviceInfo,
                    )
                )
            }
        }
    }

    override suspend fun requestPasswordReset(email: String): RequestResult<Unit> {
        val asked = safeRequest<Unit> {
            httpClientProvider.client.post("auth/password/forgot") {
                contentType(ContentType.Application.Json)
                setBody(ForgotPasswordRequest(email = email.trim()))
            }
        }
        return when (asked) {
            is RequestResult.Error -> asked
            is RequestResult.Success -> RequestResult.Success(Unit)
        }
    }

    override suspend fun resetPasswordByEmail(
        token: String,
        password: String,
        deviceInfo: String,
    ): RequestResult<Unit> {
        return authorizeBy {
            httpClientProvider.client.post("auth/password/reset/email") {
                contentType(ContentType.Application.Json)
                setBody(
                    PasswordResetByEmailRequest(
                        token = token,
                        password = password,
                        deviceInfo = deviceInfo,
                    )
                )
            }
        }
    }

    private suspend fun authorizeBy(request: suspend () -> HttpResponse): RequestResult<Unit> {
        val authorized = safeRequest<AuthTokensResponse> { request() }
        return when (authorized) {
            is RequestResult.Error -> authorized
            is RequestResult.Success -> storeTokens(authorized.data)
        }
    }

    override suspend fun startTelegramLogin(): RequestResult<TelegramLoginStart> {
        val started = safeRequest<TelegramStartResponse> {
            httpClientProvider.client.post("auth/telegram/start")
        }
        return when (started) {
            is RequestResult.Error -> started
            is RequestResult.Success -> telegramStartOf(started.data)
        }
    }

    override suspend fun signInWithProvider(
        provider: AuthProvider,
        token: String,
        deviceInfo: String,
    ): RequestResult<Unit> {
        val signedIn = safeRequest<AuthTokensResponse> {
            httpClientProvider.client.post("auth/external") {
                contentType(ContentType.Application.Json)
                setBody(
                    ExternalSignInRequest(
                        provider = provider.name,
                        token = token,
                        deviceInfo = deviceInfo,
                    )
                )
            }
        }
        return when (signedIn) {
            is RequestResult.Error -> signedIn
            is RequestResult.Success -> storeTokens(signedIn.data)
        }
    }

    override suspend fun joinCoach(code: String): RequestResult<Unit> {
        val joined = safeRequest<Unit> {
            httpClientProvider.client.post("auth/invites/join") {
                contentType(ContentType.Application.Json)
                setBody(JoinCoachRequest(code = code.trim().uppercase()))
            }
        }
        return when (joined) {
            is RequestResult.Error -> joined
            is RequestResult.Success -> {
                sessionEvents.notifyProfileChanged()
                RequestResult.Success(Unit)
            }
        }
    }

    private fun telegramStartOf(response: TelegramStartResponse): RequestResult<TelegramLoginStart> {
        val claimToken = response.claimToken
        val deepLink = response.deepLink
        if (claimToken == null || deepLink == null) {
            logger.error(tag = LOG_TAG, message = "в ответе нет ссылки или токена входа через Telegram")
            return RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "",
                devMessage = "TelegramStartResponse is incomplete",
            )
        }
        return RequestResult.Success(TelegramLoginStart(claimToken = claimToken, deepLink = deepLink))
    }

    private fun previewOf(response: InvitePreviewResponse): RequestResult<InvitePreview> {
        val coachDisplayName = response.coachDisplayName
        val needsDisplayName = response.needsDisplayName
        if (coachDisplayName == null || needsDisplayName == null) {
            logger.error(tag = LOG_TAG, message = "приглашение без тренера или без признака имени")
            return RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "",
                devMessage = "invite preview is incomplete",
            )
        }
        return RequestResult.Success(
            InvitePreview(coachDisplayName = coachDisplayName, needsDisplayName = needsDisplayName)
        )
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
            logger.error(tag = LOG_TAG, message = "В ответе на вход нет одного из токенов")
            return RequestResult.Error(
                kind = RequestFailure.Parsing,
                statusCode = null,
                userMessage = "",
                devMessage = "Ответ на вход не содержит пары токенов",
            )
        }
        tokenStorage.write(AuthTokens(accessToken = accessToken, refreshToken = refreshToken))
        httpClientProvider.forgetAuthenticatedUser()
        sessionEvents.notifyAuthChanged()
        return RequestResult.Success(Unit)
    }
}
