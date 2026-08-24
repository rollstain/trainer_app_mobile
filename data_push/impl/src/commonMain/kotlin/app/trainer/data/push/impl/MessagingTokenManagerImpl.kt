package app.trainer.data.push.impl

import app.trainer.data.push.MessagingTokenManager
import app.trainer.data.push.NotificationsUtils
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.TokenStorage
import kotlinx.coroutines.CancellationException

private const val LOG_TAG = "push-token"

class MessagingTokenManagerImpl(
    private val notificationsUtils: NotificationsUtils,
    private val pushTokenRepository: PushTokenRepository,
    private val tokenStorage: TokenStorage,
    private val logger: Logger,
) : MessagingTokenManager {

    private var registeredToken: String? = null

    override suspend fun refreshToken() {
        if (tokenStorage.read() == null) {
            logger.info(tag = LOG_TAG, message = "Пользователь не авторизован, регистрация отложена")
            return
        }
        val messagingToken = readMessagingToken() ?: return
        if (messagingToken == registeredToken) return

        val registered = pushTokenRepository.register(
            token = messagingToken,
            platform = notificationsUtils.platform,
        )
        when (registered) {
            is RequestResult.Error -> logger.error(
                tag = LOG_TAG,
                message = "Не удалось зарегистрировать пуш-токен: ${registered.devMessage}",
            )
            is RequestResult.Success -> registeredToken = messagingToken
        }
    }

    private suspend fun readMessagingToken(): String? {
        val messagingToken = try {
            notificationsUtils.getMessagingToken()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            logger.error(tag = LOG_TAG, message = "Не удалось получить пуш-токен", throwable = failure)
            return null
        }
        if (messagingToken.isNullOrBlank()) {
            logger.info(tag = LOG_TAG, message = "Пуш-токен ещё не готов")
            return null
        }
        return messagingToken
    }
}
