package app.trainer.data.traininglog

import app.trainer.entities.RequestResult
import kotlinx.datetime.LocalDate

interface TrainingLogRepository {

    suspend fun availableExercises(): RequestResult<List<Exercise>>

    suspend fun createExercise(
        name: String,
        muscleGroup: String?,
        kind: ExerciseKind,
    ): RequestResult<Exercise>

    suspend fun ownEntries(from: LocalDate, to: LocalDate): RequestResult<List<TrainingLogEntry>>

    suspend fun clientEntries(
        clientUserId: String,
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<TrainingLogEntry>>

    suspend fun saveEntry(entryDate: LocalDate, draft: TrainingLogDraft): RequestResult<TrainingLogEntry>
}
