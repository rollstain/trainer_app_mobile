package app.trainer.android

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import app.trainer.data.push.RestTimerAlarm

const val REST_TIMER_NOTIFICATION_CHANNEL_ID = "rest_timer"

private const val REST_TIMER_REQUEST_CODE = 4201
private const val REST_TIMER_NOTIFICATION_ID = 4202
private const val REST_TIMER_ACTION = "app.trainer.android.REST_FINISHED"
private const val MILLIS_IN_SECOND = 1000L
private const val REST_FINISHED_TITLE = "Отдых закончен"
private const val REST_FINISHED_BODY = "Пора к следующему подходу"

class AndroidRestTimerAlarm(private val context: Context) : RestTimerAlarm {

    override fun schedule(afterSeconds: Int) {
        val manager = context.getSystemService<AlarmManager>() ?: return
        val triggerAt = SystemClock.elapsedRealtime() + afterSeconds * MILLIS_IN_SECOND
        if (manager.allowsExactAlarms()) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                context.restTimerIntent(),
            )
        } else {
            manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, context.restTimerIntent())
        }
    }

    override fun cancel() {
        context.getSystemService<AlarmManager>()?.cancel(context.restTimerIntent())
        NotificationManagerCompat.from(context).cancel(REST_TIMER_NOTIFICATION_ID)
    }

    private fun AlarmManager.allowsExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()
    }
}

class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != REST_TIMER_ACTION) return
        val notification = NotificationCompat.Builder(context, REST_TIMER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(REST_FINISHED_TITLE)
            .setContentText(REST_FINISHED_BODY)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(REST_TIMER_NOTIFICATION_ID, notification)
    }
}

fun Context.createRestTimerNotificationChannel() {
    val manager = getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(REST_TIMER_NOTIFICATION_CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            REST_TIMER_NOTIFICATION_CHANNEL_ID,
            getString(R.string.rest_timer_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        )
    )
}

private fun Context.restTimerIntent(): PendingIntent = PendingIntent.getBroadcast(
    this,
    REST_TIMER_REQUEST_CODE,
    Intent(REST_TIMER_ACTION).setPackage(packageName),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)
