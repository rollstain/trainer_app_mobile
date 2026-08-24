package app.trainer.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

const val CHAT_NOTIFICATION_CHANNEL_ID = "chat_messages"
const val SCHEDULE_NOTIFICATION_CHANNEL_ID = "schedule"

fun Context.createChatNotificationChannel() {
    createChannel(
        channelId = CHAT_NOTIFICATION_CHANNEL_ID,
        nameResourceId = R.string.chat_notification_channel_name,
    )
}

fun Context.createScheduleNotificationChannel() {
    createChannel(
        channelId = SCHEDULE_NOTIFICATION_CHANNEL_ID,
        nameResourceId = R.string.schedule_notification_channel_name,
    )
}

private fun Context.createChannel(channelId: String, nameResourceId: Int) {
    val manager = getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(channelId) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            channelId,
            getString(nameResourceId),
            NotificationManager.IMPORTANCE_HIGH,
        )
    )
}
