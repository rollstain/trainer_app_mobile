package app.trainer.android

import app.trainer.app.di.sharedModules
import app.trainer.data.push.NotificationsUtils
import app.trainer.data.push.RestTimerAlarm
import app.trainer.database.DatabaseDriverFactory
import app.trainer.network.impl.NetworkConfig
import app.trainer.network.impl.SecureSettingsFactory
import com.russhwolf.settings.Settings
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

private val TEST_CONFIG = NetworkConfig(
    baseUrl = "http://localhost:8080/",
    chatWebSocketUrl = "ws://localhost:8080/ws/chat",
)

class KoinGraphTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every dependency in shared graph is resolvable`() {
        val wholeGraph = module {
            includes(sharedModules(networkConfig = TEST_CONFIG, deviceInfo = "test"))
        }
        wholeGraph.verify(
            extraTypes = listOf(
                DatabaseDriverFactory::class,
                SecureSettingsFactory::class,
                Settings::class,
                NetworkConfig::class,
                String::class,
                NotificationsUtils::class,
                RestTimerAlarm::class,
            )
        )
    }
}
