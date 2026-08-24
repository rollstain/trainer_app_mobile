package app.trainer.data.chat.impl

import app.trainer.data.chat.ChatRealtime
import app.trainer.data.chat.ChatRepository
import app.trainer.logger.Logger
import app.trainer.network.HttpClientProvider
import app.trainer.network.TokenStorage
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
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
private const val RECONNECT_DELAY_MS = 3_000L

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
            delay(RECONNECT_DELAY_MS)
        }
    }

    private suspend fun listen() {
        if (tokenStorage.read() == null) {
            logger.info(tag = LOG_TAG, message = "Нет токена, подключение отложено")
            return
        }
        httpClientProvider.client.webSocket(webSocketUrl) {
            logger.info(tag = LOG_TAG, message = "Подключено")
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                handleFrame(frame.readText())
            }
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
