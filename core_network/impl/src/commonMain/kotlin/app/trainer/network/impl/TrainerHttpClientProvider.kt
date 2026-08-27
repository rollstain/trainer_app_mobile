package app.trainer.network.impl

import app.trainer.logger.Logger
import app.trainer.network.AuthTokens
import app.trainer.network.HttpClientProvider
import app.trainer.network.SessionEvents
import app.trainer.network.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val LOG_TAG = "network"
private const val REQUEST_TIMEOUT_MS = 30_000L
private const val UPLOAD_TIMEOUT_MS = 300_000L
private const val WEB_SOCKET_PING_INTERVAL_MS = 20_000L

class TrainerHttpClientProvider(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val tokenRefresher: TokenRefresher,
    private val sessionEvents: SessionEvents,
    private val logger: Logger,
) : HttpClientProvider {

    override val client: HttpClient by lazy { createClient() }

    override val plainClient: HttpClient by lazy { createPlainClient() }

    override fun forgetAuthenticatedUser() {
        client.authProvider<BearerAuthProvider>()?.clearToken()
    }

    private fun createPlainClient(): HttpClient = createPlatformHttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = UPLOAD_TIMEOUT_MS
        }
    }

    private fun createClient(): HttpClient = createPlatformHttpClient {
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }

        install(WebSockets) {
            pingIntervalMillis = WEB_SOCKET_PING_INTERVAL_MS
        }

        install(Logging) {
            level = LogLevel.INFO
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    this@TrainerHttpClientProvider.logger.info(tag = LOG_TAG, message = message)
                }
            }
        }

        install(Auth) {
            bearer {
                loadTokens { tokenStorage.read()?.toBearerTokens() }
                refreshTokens {
                    val current = tokenStorage.read() ?: return@refreshTokens null
                    val refreshed = tokenRefresher.refresh(current)
                    if (refreshed == null) {
                        tokenStorage.clear()
                        sessionEvents.notifyExpired()
                        null
                    } else {
                        tokenStorage.write(refreshed)
                        refreshed.toBearerTokens()
                    }
                }
            }
        }

        defaultRequest {
            url(baseUrl)
        }
    }
}

private fun AuthTokens.toBearerTokens(): BearerTokens {
    return BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
}
