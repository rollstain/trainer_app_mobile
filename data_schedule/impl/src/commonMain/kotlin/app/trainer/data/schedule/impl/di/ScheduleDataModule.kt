package app.trainer.data.schedule.impl.di

import app.trainer.data.schedule.ClientScheduleRepository
import app.trainer.data.schedule.CoachScheduleRepository
import app.trainer.data.schedule.impl.ScheduleMapper
import app.trainer.data.schedule.impl.ScheduleRepositoryImpl
import org.koin.dsl.module

class ScheduleDataModule {

    val module = module {
        single { ScheduleMapper(logger = get()) }
        single { ScheduleRepositoryImpl(httpClientProvider = get(), mapper = get()) }
        single<CoachScheduleRepository> { get<ScheduleRepositoryImpl>() }
        single<ClientScheduleRepository> { get<ScheduleRepositoryImpl>() }
    }
}
