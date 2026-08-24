package app.trainer.data.schedule.impl.di

import app.trainer.data.schedule.ScheduleRepository
import app.trainer.data.schedule.impl.ScheduleMapper
import app.trainer.data.schedule.impl.ScheduleRepositoryImpl
import org.koin.dsl.module

class ScheduleDataModule {

    val module = module {
        single { ScheduleMapper(logger = get()) }
        single<ScheduleRepository> {
            ScheduleRepositoryImpl(httpClientProvider = get(), mapper = get())
        }
    }
}
