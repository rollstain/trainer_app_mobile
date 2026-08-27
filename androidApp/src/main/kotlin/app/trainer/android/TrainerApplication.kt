package app.trainer.android

import android.app.Application
import app.trainer.app.SessionController
import app.trainer.app.di.initKoin
import app.trainer.data.push.NotificationsUtils
import app.trainer.data.push.RestTimerAlarm
import app.trainer.data.push.impl.AndroidNotificationsUtils
import app.trainer.database.DatabaseDriverFactory
import app.trainer.logger.ConsoleLogger
import app.trainer.logger.Logger
import app.trainer.network.impl.NetworkConfig
import app.trainer.network.impl.SecureSettingsFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.dsl.module

class TrainerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val koin = initKoin(
            deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            networkConfig = NetworkConfig(
                baseUrl = BuildConfig.BASE_URL,
                chatWebSocketUrl = BuildConfig.CHAT_WEB_SOCKET_URL,
            ),
            platformModule = module {
                single { DatabaseDriverFactory(context = applicationContext) }
                single { SecureSettingsFactory(context = applicationContext) }
                single<Logger> {
                    CrashlyticsLogger(
                        delegate = ConsoleLogger(),
                        crashlytics = FirebaseCrashlytics.getInstance(),
                    )
                }
                single<NotificationsUtils> { AndroidNotificationsUtils(logger = get()) }
                single<RestTimerAlarm> { AndroidRestTimerAlarm(context = applicationContext) }
            },
        )
        createChatNotificationChannel()
        createRestTimerNotificationChannel()
        createScheduleNotificationChannel()
        koin.koin.get<SessionController>().start()
    }
}
