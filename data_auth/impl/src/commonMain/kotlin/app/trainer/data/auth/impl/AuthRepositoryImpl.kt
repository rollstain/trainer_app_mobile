package app.trainer.data.auth.impl

import app.trainer.data.auth.AuthProvider
import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.CoachAccessStatus
import app.trainer.data.auth.CoachRequest
import app.trainer.data.auth.CoachRequestsRepository
import app.trainer.data.auth.DeviceSession
import app.trainer.data.auth.DeviceSessionsRepository
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.data.auth.InviteCode
import app.trainer.data.auth.InvitePreview
import app.trainer.data.auth.LinkedIdentity
import app.trainer.data.auth.TelegramLoginStart
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.AuthTokens
import app.trainer.network.HttpClientProvider
import app.trainer.network.SessionEvents
import app.trainer.network.TokenStorage
import app.trainer.network.safeRequest
import io.ktor.client.request.delete
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
) : AuthRepository, IdentitiesRepository, DeviceSessionsRepository, CoachRequestsRepository {

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

    override suspend fun coachAccessStatus(): RequestResult<CoachAccessStatus> =
        coachAccessOf { httpClientProvider.client.get("me/coach-request") }

    override suspend fun askCoachAccess(): RequestResult<CoachAccessStatus> =
        coachAccessOf { httpClientProvider.client.post("me/coach-request") }

    private suspend fun coachAccessOf(request: suspend () -> HttpResponse): RequestResult<CoachAccessStatus> {
        val loaded = safeRequest<CoachAccessStatusResponse> { request() }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(
                CoachAccessStatus.entries.firstOrNull { it.name == loaded.data.status }
                    ?: CoachAccessStatus.NONE
            )
        }
    }

    override suspend fun pending(): RequestResult<List<CoachRequest>> {
        val loaded = safeRequest<List<CoachRequestResponse>> {
            httpClientProvider.client.get("owner/coach-requests")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(::toCoachRequest))
        }
    }

    override suspend fun approve(requestId: String): RequestResult<Unit> =
        decided { httpClientProvider.client.post("owner/coach-requests/$requestId/approve") }

    override suspend fun decline(requestId: String): RequestResult<Unit> =
        decided { httpClientProvider.client.post("owner/coach-requests/$requestId/decline") }

    private suspend fun decided(request: suspend () -> HttpResponse): RequestResult<Unit> {
        val decided = safeRequest<Unit> { request() }
        return when (decided) {
            is RequestResult.Error -> decided
            is RequestResult.Success -> RequestResult.Success(Unit)
        }
    }

    private fun toCoachRequest(response: CoachRequestResponse): CoachRequest? {
        val id = response.id ?: return null
        val displayName = response.displayName ?: return null
        val createdAt = response.createdAt ?: return null
        return CoachRequest(id = id, displayName = displayName, createdAtIso = createdAt)
    }

    override suspend fun linkedIdentities(): RequestResult<List<LinkedIdentity>> {
        return identitiesOf { httpClientProvider.client.get("me/identities") }
    }

    override suspend fun linkProvider(provider: AuthProvider, token: String): RequestResult<List<LinkedIdentity>> {
        return identitiesOf {
            httpClientProvider.client.post("me/identities") {
                contentType(ContentType.Application.Json)
                setBody(LinkIdentityRequest(provider = provider.name, token = token))
            }
        }
    }

    override suspend fun unlinkProvider(provider: AuthProvider): RequestResult<List<LinkedIdentity>> {
        return identitiesOf { httpClientProvider.client.delete("me/identities/${provider.name}") }
    }

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

    private suspend fun identitiesOf(request: suspend () -> HttpResponse): RequestResult<List<LinkedIdentity>> {
        val loaded = safeRequest<List<LinkedIdentityResponse>> { request() }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(::toIdentity))
        }
    }

    private fun toIdentity(response: LinkedIdentityResponse): LinkedIdentity? {
        val provider = response.provider?.let { name -> AuthProvider.entries.firstOrNull { it.name == name } }
        val linkedAt = response.linkedAt
        if (provider == null || linkedAt == null) {
            logger.error(tag = LOG_TAG, message = "Пропущена привязка без провайдера или даты")
            return null
        }
        return LinkedIdentity(provider = provider, linkedAtIso = linkedAt)
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
