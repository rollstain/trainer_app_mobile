package app.trainer.data.traininglog.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.trainer.database.TrainerDatabase
import app.trainer.logger.Logger
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

private const val LOG_TAG = "training-log-outbox"

class TrainingLogOutbox(
    private val database: TrainerDatabase,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val queries get() = database.trainingLogOutboxQueries
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun enqueue(entryDate: LocalDate, request: SaveTrainingLogRequest) {
        withContext(ioDispatcher) {
            queries.upsertQueued(
                dateIso = entryDate.toString(),
                payloadJson = json.encodeToString(request),
                queuedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }

    suspend fun remove(entryDate: LocalDate) {
        withContext(ioDispatcher) { queries.deleteQueued(entryDate.toString()) }
    }

    suspend fun clear() {
        withContext(ioDispatcher) { queries.deleteAllQueued() }
    }

    suspend fun queued(): List<Pair<LocalDate, SaveTrainingLogRequest>> = withContext(ioDispatcher) {
        queries.selectQueued().executeAsList().mapNotNull { row ->
            val entryDate = runCatching { LocalDate.parse(row.dateIso) }.getOrNull()
            val request = runCatching {
                json.decodeFromString<SaveTrainingLogRequest>(row.payloadJson)
            }.getOrNull()
            if (entryDate == null || request == null) {
                logger.error(tag = LOG_TAG, message = "Запись очереди ${row.dateIso} не разобрана, удалена")
                queries.deleteQueued(row.dateIso)
                null
            } else {
                entryDate to request
            }
        }
    }

    fun observeQueuedDates(): Flow<Set<LocalDate>> = queries.selectQueuedDates()
        .asFlow()
        .mapToList(ioDispatcher)
        .map { rows -> rows.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet() }
}
