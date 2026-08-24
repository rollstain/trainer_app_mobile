package app.trainer.data.push.impl

import app.trainer.data.push.NotificationsUtils
import app.trainer.data.push.PushPlatform
import app.trainer.logger.Logger
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val LOG_TAG = "push-token"

class AndroidNotificationsUtils(private val logger: Logger) : NotificationsUtils {

    override val platform: PushPlatform = PushPlatform.ANDROID

    override suspend fun getMessagingToken(): String? {
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrElse { failure ->
            logger.error(
                tag = LOG_TAG,
                message = "Firebase не инициализирован, нет google-services.json",
                throwable = failure,
            )
            return null
        }
        return suspendCancellableCoroutine { continuation ->
            messaging.token.addOnCompleteListener { task ->
                val failure = task.exception
                when {
                    task.isSuccessful -> continuation.resume(task.result)
                    failure != null -> continuation.resumeWithException(failure)
                    else -> continuation.resume(null)
                }
            }
        }
    }
}
