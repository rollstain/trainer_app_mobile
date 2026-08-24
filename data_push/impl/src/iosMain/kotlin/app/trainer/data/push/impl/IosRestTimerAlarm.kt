package app.trainer.data.push.impl

import app.trainer.data.push.RestTimerAlarm
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

private const val REST_TIMER_REQUEST_ID = "rest_timer"
private const val REST_FINISHED_TITLE = "Отдых закончен"
private const val REST_FINISHED_BODY = "Пора к следующему подходу"

class IosRestTimerAlarm : RestTimerAlarm {

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(afterSeconds: Int) {
        cancel()
        val content = UNMutableNotificationContent().apply {
            setTitle(REST_FINISHED_TITLE)
            setBody(REST_FINISHED_BODY)
            setSound(UNNotificationSound.defaultSound())
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = afterSeconds.toDouble(),
            repeats = false,
        )
        center.addNotificationRequest(
            request = UNNotificationRequest.requestWithIdentifier(
                identifier = REST_TIMER_REQUEST_ID,
                content = content,
                trigger = trigger,
            ),
            withCompletionHandler = null,
        )
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REST_TIMER_REQUEST_ID))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(REST_TIMER_REQUEST_ID))
    }
}
