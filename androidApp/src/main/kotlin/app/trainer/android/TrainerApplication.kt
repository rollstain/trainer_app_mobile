package app.trainer.android

import android.app.Application
import app.trainer.app.AppStartup
import app.trainer.app.di.initKoin
import app.trainer.data.push.NotificationsUtils
import app.trainer.data.push.RestTimerAlarm
import app.trainer.data.push.impl.AndroidNotificationsUtils
import app.trainer.database.DatabaseDriverFactory
import app.trainer.network.impl.NetworkConfig
import app.trainer.network.impl.SecureSettingsFactory
import org.koin.dsl.module

private const val BASE_URL = "http://10.0.2.2:8080/"
private const val CHAT_WEB_SOCKET_URL = "ws://10.0.2.2:8080/ws/chat"

class TrainerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val koin = initKoin(
            deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            networkConfig = NetworkConfig(
                baseUrl = BASE_URL,
                chatWebSocketUrl = CHAT_WEB_SOCKET_URL,
            ),
            platformModule = module {
                single { DatabaseDriverFactory(context = applicationContext) }
                single { SecureSettingsFactory(context = applicationContext) }
                single<NotificationsUtils> { AndroidNotificationsUtils(logger = get()) }
                single<RestTimerAlarm> { AndroidRestTimerAlarm(context = applicationContext) }
            },
        )
        createChatNotificationChannel()
        createRestTimerNotificationChannel()
        createScheduleNotificationChannel()
        koin.koin.get<AppStartup>().onAppStarted()
    }
}
