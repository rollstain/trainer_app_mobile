package app.trainer.data.program.impl.di

import app.trainer.data.program.ProgramRepository
import app.trainer.data.program.impl.ProgramMapper
import app.trainer.data.program.impl.ProgramRepositoryImpl
import org.koin.dsl.module

class ProgramDataModule {

    val module = module {
        single { ProgramMapper(logger = get()) }
        single<ProgramRepository> {
            ProgramRepositoryImpl(httpClientProvider = get(), mapper = get())
        }
    }
}
