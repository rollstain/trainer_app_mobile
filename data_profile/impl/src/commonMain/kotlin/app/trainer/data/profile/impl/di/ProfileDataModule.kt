package app.trainer.data.profile.impl.di

import app.trainer.data.profile.ProfileRepository
import app.trainer.data.profile.impl.ProfileRepositoryImpl
import app.trainer.entities.LocalDataCleaner
import org.koin.dsl.bind
import org.koin.dsl.module

class ProfileDataModule {

    val module = module {
        single<ProfileRepository> {
            ProfileRepositoryImpl(
                settings = get(),
                ioDispatcher = get(),
                httpClientProvider = get(),
                logger = get(),
            )
        } bind LocalDataCleaner::class
    }
}
