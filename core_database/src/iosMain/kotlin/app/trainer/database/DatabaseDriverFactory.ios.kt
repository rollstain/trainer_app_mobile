package app.trainer.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

private const val DATABASE_NAME = "trainer.db"

actual class DatabaseDriverFactory {

    actual fun create(): SqlDriver {
        return NativeSqliteDriver(schema = TrainerDatabase.Schema, name = DATABASE_NAME)
    }
}
