package app.trainer.data.profile.impl.di

import app.trainer.data.profile.ProfileRepository
import app.trainer.data.profile.impl.ProfileRepositoryImpl
import org.koin.dsl.module

class ProfileDataModule {

    val module = module {
        single<ProfileRepository> {
            ProfileRepositoryImpl(httpClientProvider = get(), logger = get())
        }
    }
}
