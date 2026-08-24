package app.trainer.data.push.impl

import app.trainer.data.push.NotificationsUtils
import app.trainer.data.push.PushPlatform

class IosNotificationsUtilsHolder {

    fun setInstance(notificationsUtils: NotificationsUtils) {
        instance = notificationsUtils
    }

    companion object {

        var instance: NotificationsUtils? = null
    }
}

class MissingSwiftNotificationsUtils : NotificationsUtils {

    override val platform: PushPlatform = PushPlatform.IOS

    override suspend fun getMessagingToken(): String? = null
}
