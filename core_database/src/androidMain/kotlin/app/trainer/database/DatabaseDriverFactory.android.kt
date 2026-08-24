package app.trainer.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

private const val DATABASE_NAME = "trainer.db"

actual class DatabaseDriverFactory(private val context: Context) {

    actual fun create(): SqlDriver {
        return AndroidSqliteDriver(
            schema = TrainerDatabase.Schema,
            context = context,
            name = DATABASE_NAME,
        )
    }
}
