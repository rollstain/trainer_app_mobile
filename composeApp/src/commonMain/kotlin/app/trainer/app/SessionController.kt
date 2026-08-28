package app.trainer.app

import app.trainer.data.auth.AuthRepository
import app.trainer.data.chat.ChatRealtime
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.push.MessagingTokenManager
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.LocalDataCleaner
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.logger.Logger
import app.trainer.network.SessionEvents
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

private const val LOG_TAG = "session"
private const val RETRY_BUFFER = 1

sealed interface SessionStatus {

    data object Loading : SessionStatus

    data class SignedOut(val afterSessionExpiry: Boolean) : SessionStatus

    data class ProfileUnavailable(val failure: RequestResult.Error) : SessionStatus

    data class SignedIn(val isCoach: Boolean, val hasCoach: Boolean) : SessionStatus
}

class SessionController(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val chatRealtime: ChatRealtime,
    private val messagingTokenManager: MessagingTokenManager,
    private val trainingLogRepository: TrainingLogRepository,
    private val sessionEvents: SessionEvents,
    private val localDataCleaners: List<LocalDataCleaner>,
    private val logger: Logger,
    ioDispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val mutableStatus = MutableStateFlow<SessionStatus>(SessionStatus.Loading)
    private val retries = MutableSharedFlow<Unit>(
        extraBufferCapacity = RETRY_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val status: StateFlow<SessionStatus> = mutableStatus

    fun start() {
        scope.launch {
            merge(
                flowOf(Unit),
                sessionEvents.authChanged,
                sessionEvents.profileChanged,
                sessionEvents.expired,
                retries,
            )
                .collectLatest { evaluate() }
        }
    }

    fun retry() {
        retries.tryEmit(Unit)
    }

    private suspend fun evaluate() {
        val previous = mutableStatus.value
        if (!authRepository.isAuthorized()) {
            val wasSignedIn = previous is SessionStatus.SignedIn
            mutableStatus.value = SessionStatus.SignedOut(afterSessionExpiry = wasSignedIn)
            if (wasSignedIn) tearDownAuthorizedWork()
            return
        }
        showLastKnownRole()
        val beforeProfileLoad = mutableStatus.value
        when (val profile = profileRepository.me()) {
            is RequestResult.Success -> {
                mutableStatus.value = SessionStatus.SignedIn(
                    isCoach = profile.data.coachId != null,
                    hasCoach = profile.data.hasCoach,
                )
                if (beforeProfileLoad !is SessionStatus.SignedIn) startAuthorizedWork()
            }
            is RequestResult.Error -> {
                val staysOffline = beforeProfileLoad is SessionStatus.SignedIn &&
                    profile.kind == RequestFailure.Network
                if (staysOffline) {
                    logger.info(tag = LOG_TAG, message = "Сети нет, работаем на последней известной роли")
                } else {
                    logger.error(tag = LOG_TAG, message = "Профиль не загрузился, роль неизвестна")
                    mutableStatus.value = SessionStatus.ProfileUnavailable(failure = profile)
                }
            }
        }
    }

    private suspend fun showLastKnownRole() {
        if (mutableStatus.value is SessionStatus.SignedIn) return
        val isCoach = profileRepository.lastKnownIsCoach() ?: return
        mutableStatus.value = SessionStatus.SignedIn(isCoach = isCoach, hasCoach = true)
        startAuthorizedWork()
    }

    private suspend fun startAuthorizedWork() {
        chatRealtime.start()
        messagingTokenManager.refreshToken()
        trainingLogRepository.sendQueuedEntries()
    }

    private suspend fun tearDownAuthorizedWork() {
        chatRealtime.stop()
        localDataCleaners.forEach { cleaner -> cleaner.clearLocalData() }
    }
}
