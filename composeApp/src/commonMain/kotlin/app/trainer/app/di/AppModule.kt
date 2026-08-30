package app.trainer.app.di

import app.trainer.app.PendingEmailConfirmation
import app.trainer.app.PendingInvite
import app.trainer.app.PendingPasswordReset
import app.trainer.app.SessionController
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.input.WeightInput
import app.trainer.data.auth.FreshSignUp
import app.trainer.data.chat.impl.di.ChatDataModule
import app.trainer.feature.account.di.AccountFeatureModule
import app.trainer.network.impl.NetworkConfig
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

class AppModule(private val config: NetworkConfig, private val deviceInfo: String) {

    val module = module {
        single<CoroutineDispatcher> { ioDispatcher() }
        single { WeightInput() }
        single { PendingInvite() }
        single { PendingPasswordReset() }
        single { PendingEmailConfirmation() }
        single { FreshSignUp() }
        single { ScheduleWeeks() }
        single(named(AccountFeatureModule.DEVICE_INFO_QUALIFIER)) { deviceInfo }
        single(named(ChatDataModule.WEB_SOCKET_URL_QUALIFIER)) { config.chatWebSocketUrl }
        single {
            SessionController(
                authRepository = get(),
                profileRepository = get(),
                chatRealtime = get(),
                messagingTokenManager = get(),
                trainingLogRepository = get(),
                sessionEvents = get(),
                localDataCleaners = getAll(),
                logger = get(),
                ioDispatcher = get(),
            )
        }
    }
}
