package app.trainer.data.auth.impl.di

import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.impl.AuthRepositoryImpl
import org.koin.dsl.module

class AuthDataModule {

    val module = module {
        single<AuthRepository> {
            AuthRepositoryImpl(httpClientProvider = get(), tokenStorage = get(), logger = get())
        }
    }
}
