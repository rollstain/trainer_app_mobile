package app.trainer.data.traininglog

import app.trainer.entities.LocalDataCleaner
import app.trainer.entities.Paged
import app.trainer.entities.RequestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

sealed interface SaveOutcome {

    data class Sent(val entry: TrainingLogEntry) : SaveOutcome

    data object Queued : SaveOutcome
}

interface TrainingLogRepository : LocalDataCleaner {

    suspend fun availableExercises(limit: Int? = null, after: String? = null): RequestResult<Paged<List<Exercise>>>

    suspend fun createExercise(
        name: String,
        muscleGroup: String?,
        kind: ExerciseKind,
        description: String?,
        videoUrl: String?,
    ): RequestResult<Exercise>

    suspend fun ownEntries(from: LocalDate, to: LocalDate): RequestResult<List<TrainingLogEntry>>

    suspend fun clientEntries(
        clientUserId: String,
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<TrainingLogEntry>>

    suspend fun saveEntry(entryDate: LocalDate, draft: TrainingLogDraft): RequestResult<SaveOutcome>

    suspend fun sendQueuedEntries()

    fun observeQueuedDates(): Flow<Set<LocalDate>>
}
