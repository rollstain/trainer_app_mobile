package app.trainer.data.traininglog.impl

import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.TrainingDayInput
import app.trainer.data.traininglog.TrainingInputRow
import app.trainer.data.traininglog.TrainingInputStore
import app.trainer.database.TrainerDatabase
import app.trainer.database.TrainingDraftSetEntity
import app.trainer.logger.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

private const val LOG_TAG = "training-input-store"

class TrainingInputStoreImpl(
    private val database: TrainerDatabase,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher,
) : TrainingInputStore {

    private val queries get() = database.trainingDraftQueries

    override suspend fun load(entryDate: LocalDate): TrainingDayInput? = withContext(ioDispatcher) {
        val dateIso = entryDate.toString()
        val stored = queries.selectDraft(dateIso).executeAsOneOrNull() ?: return@withContext null
        TrainingDayInput(
            notes = stored.notes,
            rows = queries.selectDraftSets(dateIso).executeAsList().mapNotNull(::toRow),
        )
    }

    override suspend fun save(entryDate: LocalDate, input: TrainingDayInput) = withContext(ioDispatcher) {
        val dateIso = entryDate.toString()
        queries.transaction {
            queries.upsertDraft(dateIso = dateIso, notes = input.notes)
            queries.deleteDraftSets(dateIso)
            input.rows.forEachIndexed { index, row ->
                queries.upsertDraftSet(
                    rowId = row.rowId,
                    dateIso = dateIso,
                    position = index.toLong(),
                    exerciseId = row.exerciseId,
                    exerciseName = row.exerciseName,
                    kind = row.kind.name,
                    repetitionsText = row.repetitionsText,
                    weightText = row.weightText,
                    durationText = row.durationText,
                    distanceText = row.distanceText,
                )
            }
        }
    }

    override suspend fun clear(entryDate: LocalDate) = withContext(ioDispatcher) {
        val dateIso = entryDate.toString()
        queries.transaction {
            queries.deleteDraftSets(dateIso)
            queries.deleteDraft(dateIso)
        }
    }

    override suspend fun clearLocalData() = withContext(ioDispatcher) {
        queries.transaction {
            queries.deleteAllDraftSets()
            queries.deleteAllDrafts()
        }
    }

    private fun toRow(stored: TrainingDraftSetEntity): TrainingInputRow? {
        val kind = ExerciseKind.entries.firstOrNull { it.name == stored.kind }
        if (kind == null) {
            logger.error(tag = LOG_TAG, message = "Пропущена строка черновика: неизвестный тип ${stored.kind}")
            return null
        }
        return TrainingInputRow(
            rowId = stored.rowId,
            exerciseId = stored.exerciseId,
            exerciseName = stored.exerciseName,
            kind = kind,
            repetitionsText = stored.repetitionsText,
            weightText = stored.weightText,
            durationText = stored.durationText,
            distanceText = stored.distanceText,
        )
    }
}
