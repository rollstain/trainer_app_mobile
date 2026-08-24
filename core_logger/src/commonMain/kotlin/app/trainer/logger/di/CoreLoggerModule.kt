package app.trainer.logger.di

import app.trainer.logger.ConsoleLogger
import app.trainer.logger.Logger
import org.koin.dsl.module

class CoreLoggerModule {

    val module = module {
        single<Logger> { ConsoleLogger() }
    }
}
