package app.trainer.database.di

import app.trainer.database.DatabaseDriverFactory
import app.trainer.database.TrainerDatabase
import org.koin.dsl.module

class CoreDatabaseModule {

    val module = module {
        single { TrainerDatabase(driver = get<DatabaseDriverFactory>().create()) }
    }
}
