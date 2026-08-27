package app.trainer.app.di

import app.trainer.data.auth.impl.di.AuthDataModule
import app.trainer.data.chat.impl.di.ChatDataModule
import app.trainer.data.clients.impl.di.ClientsDataModule
import app.trainer.data.profile.impl.di.ProfileDataModule
import app.trainer.data.program.impl.di.ProgramDataModule
import app.trainer.data.progress.impl.di.ProgressDataModule
import app.trainer.data.push.impl.di.PushDataModule
import app.trainer.data.schedule.impl.di.ScheduleDataModule
import app.trainer.data.traininglog.impl.di.TrainingLogDataModule
import app.trainer.database.di.CoreDatabaseModule
import app.trainer.feature.account.di.AccountFeatureModule
import app.trainer.feature.chat.di.ChatFeatureModule
import app.trainer.feature.clientcard.di.ClientCardFeatureModule
import app.trainer.feature.home.di.HomeFeatureModule
import app.trainer.feature.progress.di.ProgressFeatureModule
import app.trainer.feature.schedule.di.ScheduleFeatureModule
import app.trainer.feature.traininglog.di.TrainingLogFeatureModule
import app.trainer.logger.di.CoreLoggerModule
import app.trainer.network.impl.NetworkConfig
import app.trainer.network.impl.di.CoreNetworkModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun sharedModules(networkConfig: NetworkConfig, deviceInfo: String): List<Module> = listOf(
    AppModule(config = networkConfig, deviceInfo = deviceInfo).module,
    CoreLoggerModule().module,
    CoreNetworkModule(config = networkConfig).module,
    CoreDatabaseModule().module,
    AuthDataModule().module,
    ProfileDataModule().module,
    ProgressDataModule().module,
    ProgramDataModule().module,
    PushDataModule().module,
    TrainingLogDataModule().module,
    ChatDataModule().module,
    ClientsDataModule().module,
    ScheduleDataModule().module,
    AccountFeatureModule().module,
    ChatFeatureModule().module,
    ClientCardFeatureModule().module,
    HomeFeatureModule().module,
    ProgressFeatureModule().module,
    TrainingLogFeatureModule().module,
    ScheduleFeatureModule().module,
)

fun initKoin(
    networkConfig: NetworkConfig,
    deviceInfo: String,
    platformModule: Module,
): KoinApplication = startKoin {
    modules(sharedModules(networkConfig = networkConfig, deviceInfo = deviceInfo) + platformModule)
}
