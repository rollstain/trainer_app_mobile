package app.trainer.data.traininglog.impl.di

import app.trainer.data.traininglog.RestIntervalStore
import app.trainer.data.traininglog.TrainingInputStore
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.data.traininglog.impl.RestIntervalStoreImpl
import app.trainer.data.traininglog.impl.TrainingInputStoreImpl
import app.trainer.data.traininglog.impl.TrainingLogMapper
import app.trainer.data.traininglog.impl.TrainingLogOutbox
import app.trainer.data.traininglog.impl.TrainingLogRepositoryImpl
import app.trainer.entities.LocalDataCleaner
import org.koin.dsl.bind
import org.koin.dsl.module

class TrainingLogDataModule {

    val module = module {
        single { TrainingLogMapper(logger = get()) }
        single { TrainingLogOutbox(database = get(), logger = get(), ioDispatcher = get()) }
        single<TrainingLogRepository> {
            TrainingLogRepositoryImpl(
                httpClientProvider = get(),
                mapper = get(),
                outbox = get(),
                logger = get(),
            )
        } bind LocalDataCleaner::class
        single<TrainingInputStore> {
            TrainingInputStoreImpl(database = get(), logger = get(), ioDispatcher = get())
        } bind LocalDataCleaner::class
        single<RestIntervalStore> {
            RestIntervalStoreImpl(database = get(), ioDispatcher = get())
        } bind LocalDataCleaner::class
    }
}
