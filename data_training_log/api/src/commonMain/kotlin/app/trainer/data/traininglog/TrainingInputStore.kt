package app.trainer.data.traininglog

import app.trainer.entities.LocalDataCleaner
import kotlinx.datetime.LocalDate

data class TrainingInputRow(
    val rowId: String,
    val exerciseId: String,
    val exerciseName: String,
    val kind: ExerciseKind,
    val repetitionsText: String,
    val weightText: String,
    val durationText: String,
    val distanceText: String,
)

data class TrainingDayInput(
    val notes: String,
    val rows: List<TrainingInputRow>,
)

interface TrainingInputStore : LocalDataCleaner {

    suspend fun load(entryDate: LocalDate): TrainingDayInput?

    suspend fun save(entryDate: LocalDate, input: TrainingDayInput)

    suspend fun clear(entryDate: LocalDate)
}
