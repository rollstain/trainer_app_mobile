package app.trainer.data.traininglog.impl

import app.trainer.data.traininglog.RestIntervalStore
import app.trainer.database.TrainerDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class RestIntervalStoreImpl(
    private val database: TrainerDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : RestIntervalStore {

    private val queries get() = database.restIntervalQueries

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
