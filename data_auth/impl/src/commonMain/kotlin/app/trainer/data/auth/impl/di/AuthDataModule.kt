package app.trainer.data.auth.impl.di

import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.DeviceSessionsRepository
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.data.auth.impl.AuthRepositoryImpl
import app.trainer.data.auth.impl.DeviceSessionsRepositoryImpl
import app.trainer.data.auth.impl.LoginMethodsRepositoryImpl
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
        single<IdentitiesRepository> {
            LoginMethodsRepositoryImpl(httpClientProvider = get(), logger = get())
        }
        single<DeviceSessionsRepository> {
            DeviceSessionsRepositoryImpl(httpClientProvider = get(), logger = get())
        }
    }
}
