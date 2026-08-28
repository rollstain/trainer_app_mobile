package app.trainer.data.progress.impl.di

import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.FormCheckRepository
import app.trainer.data.progress.HabitsRepository
import app.trainer.data.progress.impl.CheckInRepositoryImpl
import app.trainer.data.progress.impl.FormCheckRepositoryImpl
import app.trainer.data.progress.impl.HabitsRepositoryImpl
import app.trainer.data.progress.impl.ProgressMapper
import org.koin.dsl.module

class ProgressDataModule {

    val module = module {
        single { ProgressMapper(logger = get()) }
        single<CheckInRepository> {
            CheckInRepositoryImpl(
                httpClientProvider = get(),
                mapper = get(),
                presignedUploader = get(),
            )
        }
        single<FormCheckRepository> {
            FormCheckRepositoryImpl(
                httpClientProvider = get(),
                mapper = get(),
                presignedUploader = get(),
                logger = get(),
            )
        }
        single<HabitsRepository> {
            HabitsRepositoryImpl(httpClientProvider = get(), mapper = get())
        }
    }
}
