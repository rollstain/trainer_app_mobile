package app.trainer.data.auth.impl

import app.trainer.data.auth.AuthProvider
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.data.auth.LinkedIdentity
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.HttpClientProvider
import app.trainer.network.safeRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val LOG_TAG = "login-methods"

class LoginMethodsRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val logger: Logger,
) : IdentitiesRepository {

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

    override suspend fun setPassword(
        email: String?,
        login: String?,
        currentPassword: String?,
        newPassword: String,
    ): RequestResult<Unit> {
        val saved = safeRequest<Unit> {
            httpClientProvider.client.put("me/password") {
                contentType(ContentType.Application.Json)
                setBody(
                    SetPasswordRequest(
                        email = email?.trim()?.ifEmpty { null },
                        login = login?.trim()?.ifEmpty { null },
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                    )
                )
            }
        }
        return when (saved) {
            is RequestResult.Error -> saved
            is RequestResult.Success -> RequestResult.Success(Unit)
        }
    }

    override suspend fun confirmEmail(token: String): RequestResult<Unit> {
        return safeRequest<Unit> {
            httpClientProvider.client.post("auth/email/confirm") {
                contentType(ContentType.Application.Json)
                setBody(ConfirmEmailRequest(token = token))
            }
        }
    }

    override suspend fun requestEmailConfirmation(): RequestResult<Unit> {
        return safeRequest<Unit> {
            httpClientProvider.client.post("me/email/confirm-request")
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
}
