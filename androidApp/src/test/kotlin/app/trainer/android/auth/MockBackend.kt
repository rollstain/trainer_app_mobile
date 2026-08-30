package app.trainer.android.auth

import app.trainer.logger.Logger
import app.trainer.network.AuthTokens
import app.trainer.network.HttpClientProvider
import app.trainer.network.SessionEvents
import app.trainer.network.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://api.example.test/"

data class Call(val method: String, val path: String, val query: String, val body: String)

data class Reply(val status: HttpStatusCode, val body: String, val headers: Map<String, String> = emptyMap())

/**
 * Отвечает только на то, что тест разрешил явно. Незамоканный запрос роняет тест —
 * иначе забытая ручка молча вернула бы пустоту и путь считался бы пройденным.
 */
class MockBackend {

    val calls = mutableListOf<Call>()

    private val replies = mutableMapOf<String, ArrayDeque<Reply>>()

    fun on(
        method: String,
        path: String,
        status: HttpStatusCode,
        body: String = "{}",
        headers: Map<String, String> = emptyMap(),
    ) {
        replies.getOrPut(key(method, path)) { ArrayDeque() }.addLast(Reply(status, body, headers))
    }

    fun pathsCalled(): List<String> = calls.map { "${it.method} ${it.path}" }

    fun bodyOf(method: String, path: String): String =
        calls.first { it.method == method && it.path == path }.body

    fun queriesOf(method: String, path: String): List<String> =
        calls.filter { it.method == method && it.path == path }.map { it.query }

    private fun key(method: String, path: String) = "$method $path"

    private val engine = MockEngine { request ->
        val method = request.method.value
        val path = request.url.encodedPath
        val body = (request.body as? TextContent)?.text.orEmpty()
        calls += Call(method = method, path = path, query = request.url.encodedQuery, body = body)

        val queued = replies[key(method, path)]
        val reply = queued?.removeFirstOrNull()
            ?: error("Запрос к незамоканной ручке: $method $path")
        respond(
            content = reply.body,
            status = reply.status,
            headers = headersOf(
                *(reply.headers + mapOf("Content-Type" to ContentType.Application.Json.toString()))
                    .map { (name, value) -> name to listOf(value) }
                    .toTypedArray()
            ),
        )
    }

    val httpClientProvider: HttpClientProvider = object : HttpClientProvider {

        private val http = HttpClient(engine) {
            expectSuccess = false
            defaultRequest { url(BASE_URL) }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }
                )
            }
        }

        override val client: HttpClient = http

        override val plainClient: HttpClient = http

        override fun forgetAuthenticatedUser() = Unit
    }
}

class RecordingTokenStorage : TokenStorage {

    var tokens: AuthTokens? = null
        private set

    override suspend fun read(): AuthTokens? = tokens

    override suspend fun write(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}

class RecordingSessionEvents : SessionEvents {

    val authChanges = mutableListOf<Unit>()

    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val expired: Flow<Unit> = flow

    override val authChanged: Flow<Unit> = flow

    override val profileChanged: Flow<Unit> = flow

    override suspend fun notifyExpired() = Unit

    override suspend fun notifyAuthChanged() {
        authChanges += Unit
    }

    override suspend fun notifyProfileChanged() = Unit
}

object SilentLogger : Logger {

    override fun info(tag: String, message: String) = Unit

    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}
