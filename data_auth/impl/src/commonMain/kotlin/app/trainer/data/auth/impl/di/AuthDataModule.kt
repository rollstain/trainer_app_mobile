package app.trainer.data.auth.impl.di

import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.CoachRequestsRepository
import app.trainer.data.auth.DeviceSessionsRepository
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.data.auth.impl.AuthRepositoryImpl
import org.koin.dsl.module

class AuthDataModule {

    val module = module {
        single {
            AuthRepositoryImpl(
                httpClientProvider = get(),
                tokenStorage = get(),
                sessionEvents = get(),
                logger = get(),
            )
        }
        single<AuthRepository> { get<AuthRepositoryImpl>() }
        single<IdentitiesRepository> { get<AuthRepositoryImpl>() }
        single<DeviceSessionsRepository> { get<AuthRepositoryImpl>() }
        single<CoachRequestsRepository> { get<AuthRepositoryImpl>() }
    }
}
