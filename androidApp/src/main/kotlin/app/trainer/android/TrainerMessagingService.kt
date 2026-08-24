package app.trainer.android

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.trainer.data.push.MessagingTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val DIALOG_ID_KEY = "dialogId"
private const val SLOT_ID_KEY = "slotId"

class TrainerMessagingService : FirebaseMessagingService(), KoinComponent {

    private val messagingTokenManager: MessagingTokenManager by inject()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        scope.launch { messagingTokenManager.refreshToken() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        val title = notification.title.orEmpty()
        val body = notification.body.orEmpty()
        if (title.isEmpty() && body.isEmpty()) return
        showNotification(title = title, body = body, target = targetOf(message.data))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun targetOf(data: Map<String, String>): PushTarget {
        val dialogId = data[DIALOG_ID_KEY]
        if (dialogId != null) return PushTarget.Dialog(dialogId = dialogId)
        val slotId = data[SLOT_ID_KEY]
        if (slotId != null) return PushTarget.Slot(slotId = slotId)
        return PushTarget.App
    }

    private fun showNotification(title: String, body: String, target: PushTarget) {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            when (target) {
                is PushTarget.Dialog -> putExtra(DIALOG_ID_KEY, target.dialogId)
                is PushTarget.Slot -> putExtra(SLOT_ID_KEY, target.slotId)
                PushTarget.App -> Unit
            }
        }
        val notificationId = target.notificationId()
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, target.channelId())
            .setSmallIcon(target.smallIcon())
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching { manager.notify(notificationId, notification) }
    }
}

private sealed interface PushTarget {

    data class Dialog(val dialogId: String) : PushTarget

    data class Slot(val slotId: String) : PushTarget

    data object App : PushTarget
}

private fun PushTarget.channelId(): String = when (this) {
    is PushTarget.Dialog -> CHAT_NOTIFICATION_CHANNEL_ID
    is PushTarget.Slot -> SCHEDULE_NOTIFICATION_CHANNEL_ID
    PushTarget.App -> SCHEDULE_NOTIFICATION_CHANNEL_ID
}

private fun PushTarget.smallIcon(): Int = when (this) {
    is PushTarget.Dialog -> android.R.drawable.ic_dialog_email
    is PushTarget.Slot, PushTarget.App -> android.R.drawable.ic_menu_my_calendar
}

private fun PushTarget.notificationId(): Int = when (this) {
    is PushTarget.Dialog -> dialogId.hashCode()
    is PushTarget.Slot -> slotId.hashCode()
    PushTarget.App -> 0
}
