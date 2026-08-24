package app.trainer.data.push.impl.di

import app.trainer.data.push.MessagingTokenManager
import app.trainer.data.push.impl.MessagingTokenManagerImpl
import app.trainer.data.push.impl.PushTokenRepository
import org.koin.dsl.module

class PushDataModule {

    val module = module {
        single { PushTokenRepository(httpClientProvider = get()) }
        single<MessagingTokenManager> {
            MessagingTokenManagerImpl(
                notificationsUtils = get(),
                pushTokenRepository = get(),
                tokenStorage = get(),
                logger = get(),
            )
        }
    }
}
