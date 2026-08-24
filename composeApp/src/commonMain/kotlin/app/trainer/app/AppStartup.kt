package app.trainer.app

import app.trainer.data.auth.AuthRepository
import app.trainer.data.chat.ChatRealtime
import app.trainer.data.push.MessagingTokenManager
import app.trainer.entities.LocalDataCleaner
import app.trainer.logger.Logger
import app.trainer.network.SessionEvents
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val LOG_TAG = "startup"

class AppStartup(
    private val authRepository: AuthRepository,
    private val chatRealtime: ChatRealtime,
    private val messagingTokenManager: MessagingTokenManager,
    private val sessionEvents: SessionEvents,
    private val localDataCleaners: List<LocalDataCleaner>,
    private val logger: Logger,
    ioDispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    fun onAppStarted() {
        scope.launch { observeSessionExpiry() }
        scope.launch {
            if (!authRepository.isAuthorized()) {
                logger.info(tag = LOG_TAG, message = "Не авторизован, фоновые задачи не запускаются")
                return@launch
            }
            startAuthorizedWork()
        }
    }

    fun onSignedIn() {
        scope.launch { startAuthorizedWork() }
    }

    fun onSignedOut() {
        scope.launch { forgetSession() }
    }

    private suspend fun observeSessionExpiry() {
        sessionEvents.expired.collectLatest {
            logger.info(tag = LOG_TAG, message = "Сессия истекла, локальные данные стираются")
            forgetSession()
        }
    }

    private suspend fun startAuthorizedWork() {
        chatRealtime.start()
        messagingTokenManager.refreshToken()
    }

    private suspend fun forgetSession() {
        chatRealtime.stop()
        authRepository.logout()
        localDataCleaners.forEach { cleaner -> cleaner.clearLocalData() }
    }
}
