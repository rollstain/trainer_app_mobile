package app.trainer.data.chat.impl

import app.trainer.data.chat.ChatRealtime
import app.trainer.data.chat.ChatRepository
import app.trainer.logger.Logger
import app.trainer.network.HttpClientProvider
import app.trainer.network.TokenStorage
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val LOG_TAG = "chat-socket"
private const val RECONNECT_MIN_DELAY_MS = 1_000L
private const val RECONNECT_MAX_DELAY_MS = 60_000L
private const val RECONNECT_BACKOFF_FACTOR = 2
private val CONNECTION_CONSIDERED_STABLE_AFTER = 30.seconds

class ChatRealtimeImpl(
    private val httpClientProvider: HttpClientProvider,
    private val chatRepository: ChatRepository,
    private val tokenStorage: TokenStorage,
    private val mapper: ChatMapper,
    private val localStore: ChatLocalStore,
    private val json: Json,
    private val webSocketUrl: String,
    private val logger: Logger,
    ioDispatcher: CoroutineDispatcher,
) : ChatRealtime {

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var connection: Job? = null
    private var reconnectDelayMs = RECONNECT_MIN_DELAY_MS

    override fun start() {
        if (connection?.isActive == true) return
        connection = scope.launch { keepConnected() }
    }

    override fun stop() {
        connection?.cancel()
        connection = null
    }

    private suspend fun keepConnected() {
        while (scope.isActive) {
            try {
                catchUpAllDialogs()
                listen()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                logger.error(tag = LOG_TAG, message = "Соединение разорвано", throwable = failure)
            }
            awaitBeforeReconnect()
        }
    }

    private suspend fun awaitBeforeReconnect() {
        val plannedDelayMs = reconnectDelayMs
        reconnectDelayMs = (plannedDelayMs * RECONNECT_BACKOFF_FACTOR).coerceAtMost(RECONNECT_MAX_DELAY_MS)
        delay(plannedDelayMs / 2 + Random.nextLong(plannedDelayMs / 2 + 1))
    }

    private suspend fun listen() {
        if (tokenStorage.read() == null) {
            logger.info(tag = LOG_TAG, message = "Нет токена, подключение отложено")
            return
        }
        val attemptStarted = TimeSource.Monotonic.markNow()
        httpClientProvider.client.webSocket(webSocketUrl) {
            logger.info(tag = LOG_TAG, message = "Подключено")
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                handleFrame(frame.readText())
            }
        }
        if (attemptStarted.elapsedNow() >= CONNECTION_CONSIDERED_STABLE_AFTER) {
            reconnectDelayMs = RECONNECT_MIN_DELAY_MS
        }
    }

    private suspend fun handleFrame(payload: String) {
        val decoded = runCatching { json.decodeFromString<MessageResponse>(payload) }.getOrNull()
        if (decoded == null) {
            logger.error(tag = LOG_TAG, message = "Не разобран фрейм: $payload")
            return
        }
        val message = mapper.toMessage(decoded)
        if (message == null) {
            logger.error(tag = LOG_TAG, message = "Фрейм не отображается в сообщение")
            return
        }
        localStore.storeMessage(message)
    }

    private suspend fun catchUpAllDialogs() {
        chatRepository.refreshDialogs()
        chatRepository.observeDialogs().first().forEach { dialog ->
            chatRepository.syncMessages(dialogId = dialog.id)
        }
        chatRepository.retryPendingMessages()
    }
}
