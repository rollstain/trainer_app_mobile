package app.trainer.data.traininglog.impl

import app.trainer.data.traininglog.RestIntervalStore
import app.trainer.database.TrainerDatabase
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val DEFAULT_REST_KEY = "rest.default.seconds"
private const val DEFAULT_REST_SECONDS = 120

class RestIntervalStoreImpl(
    private val database: TrainerDatabase,
    private val settings: Settings,
    private val ioDispatcher: CoroutineDispatcher,
) : RestIntervalStore {

    private val queries get() = database.restIntervalQueries

    override suspend fun defaultSeconds(): Int = withContext(ioDispatcher) {
        settings.getInt(DEFAULT_REST_KEY, DEFAULT_REST_SECONDS)
    }

    override suspend fun rememberDefault(seconds: Int) = withContext(ioDispatcher) {
        settings.putInt(DEFAULT_REST_KEY, seconds)
    }

    override suspend fun secondsFor(exerciseId: String): Int? = withContext(ioDispatcher) {
        queries.selectRestInterval(exerciseId).executeAsOneOrNull()?.toInt()
    }

    override suspend fun remember(exerciseId: String, seconds: Int) = withContext(ioDispatcher) {
        queries.upsertRestInterval(exerciseId = exerciseId, seconds = seconds.toLong())
    }

    override suspend fun clearLocalData() = withContext(ioDispatcher) {
        queries.deleteAllRestIntervals()
    }
}
