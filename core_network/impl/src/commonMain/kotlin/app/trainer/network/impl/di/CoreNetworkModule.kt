package app.trainer.network.impl.di

import app.trainer.entities.LegalLinks
import app.trainer.network.HttpClientProvider
import app.trainer.network.PresignedUploader
import app.trainer.network.SessionEvents
import app.trainer.network.TokenStorage
import app.trainer.network.impl.HttpTokenRefresher
import app.trainer.network.impl.NetworkConfig
import app.trainer.network.impl.PlainPresignedUploader
import app.trainer.network.impl.SecureSettingsFactory
import app.trainer.network.impl.SessionEventsImpl
import app.trainer.network.impl.SettingsTokenStorage
import app.trainer.network.impl.TokenRefresher
import app.trainer.network.impl.TrainerHttpClientProvider
import kotlinx.serialization.json.Json
import org.koin.dsl.module

class CoreNetworkModule(private val config: NetworkConfig) {

    val module = module {
        single { config }
        single {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }
        single { get<SecureSettingsFactory>().create() }
        single<TokenStorage> { SettingsTokenStorage(settings = get(), ioDispatcher = get()) }
        single<SessionEvents> { SessionEventsImpl() }
        single<TokenRefresher> {
            HttpTokenRefresher(baseUrl = config.baseUrl, json = get(), logger = get())
        }
        single {
            LegalLinks(baseUrl = config.baseUrl)
        }
        single<PresignedUploader> { PlainPresignedUploader(httpClientProvider = get()) }
        single<HttpClientProvider> {
            TrainerHttpClientProvider(
                baseUrl = config.baseUrl,
                tokenStorage = get(),
                tokenRefresher = get(),
                sessionEvents = get(),
                logger = get(),
            )
        }
    }
}
