package app.trainer.data.traininglog.impl

import app.trainer.data.traininglog.ClientDiarySummary
import app.trainer.data.traininglog.DiaryDay
import app.trainer.data.traininglog.Exercise
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.LastPerformed
import app.trainer.data.traininglog.TrainingLogEntry
import app.trainer.data.traininglog.TrainingSet
import app.trainer.logger.Logger
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private const val LOG_TAG = "training-log-mapper"

class TrainingLogMapper(private val logger: Logger) {

    fun toExercise(response: ExerciseResponse): Exercise? {
        val id = response.id ?: return skipped(entity = "Exercise", field = "id")
        val name = response.name ?: return skipped(entity = "Exercise", field = "name")
        val kind = parseKind(response.kind) ?: return skipped(entity = "Exercise", field = "kind")
        val isOwnedByCoach = response.isOwnedByCoach
            ?: return skipped(entity = "Exercise", field = "isOwnedByCoach")
        return Exercise(
            id = id,
            name = name,
            muscleGroup = response.muscleGroup,
            kind = kind,
            isOwnedByCoach = isOwnedByCoach,
            description = response.description,
            videoUrl = response.videoUrl,
            lastPerformed = toLastPerformed(response),
        )
    }

    private fun toLastPerformed(response: ExerciseResponse): LastPerformed? {
        val hasAnyValue = response.lastRepetitions != null ||
            response.lastWeightGrams != null ||
            response.lastDurationSeconds != null ||
            response.lastDistanceMeters != null
        if (!hasAnyValue) return null
        return LastPerformed(
            repetitions = response.lastRepetitions,
            weightGrams = response.lastWeightGrams,
            durationSeconds = response.lastDurationSeconds,
            distanceMeters = response.lastDistanceMeters,
        )
    }

    fun toEntry(response: TrainingLogEntryResponse): TrainingLogEntry? {
        val id = response.id ?: return skipped(entity = "TrainingLogEntry", field = "id")
        val clientUserId = response.clientUserId
            ?: return skipped(entity = "TrainingLogEntry", field = "clientUserId")
        val entryDate = parseDate(response.entryDate)
            ?: return skipped(entity = "TrainingLogEntry", field = "entryDate")
        val totalVolumeGrams = response.totalVolumeGrams
            ?: return skipped(entity = "TrainingLogEntry", field = "totalVolumeGrams")
        return TrainingLogEntry(
            id = id,
            clientUserId = clientUserId,
            entryDate = entryDate,
            slotId = response.slotId,
            notes = response.notes,
            sets = response.sets.orEmpty().mapNotNull(::toSet),
            totalVolumeGrams = totalVolumeGrams,
        )
    }

    private fun toSet(response: TrainingSetResponse): TrainingSet? {
        val id = response.id ?: return skipped(entity = "TrainingSet", field = "id")
        val exerciseId = response.exerciseId ?: return skipped(entity = "TrainingSet", field = "exerciseId")
        val exerciseName = response.exerciseName
            ?: return skipped(entity = "TrainingSet", field = "exerciseName")
        val kind = parseKind(response.kind) ?: return skipped(entity = "TrainingSet", field = "kind")
        val position = response.position ?: return skipped(entity = "TrainingSet", field = "position")
        return TrainingSet(
            id = id,
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            kind = kind,
            position = position,
            repetitions = response.repetitions,
            weightGrams = response.weightGrams,
            durationSeconds = response.durationSeconds,
            distanceMeters = response.distanceMeters,
            isPersonalRecord = response.isPersonalRecord ?: false,
        )
    }

    fun toDiarySummary(response: ClientDiarySummaryResponse): ClientDiarySummary? {
        val clientUserId = response.clientUserId
            ?: return skipped(entity = "ClientDiarySummary", field = "clientUserId")
        val displayName = response.displayName
            ?: return skipped(entity = "ClientDiarySummary", field = "displayName")
        return ClientDiarySummary(
            clientUserId = clientUserId,
            displayName = displayName,
            linkedAt = parseInstant(response.linkedAt),
            lastEntryDate = parseDate(response.lastEntryDate),
            days = response.days.orEmpty().mapNotNull(::toDiaryDay),
        )
    }

    private fun toDiaryDay(response: DiaryDayResponse): DiaryDay? {
        val entryDate = parseDate(response.entryDate) ?: return skipped(entity = "DiaryDay", field = "entryDate")
        val volumeGrams = response.volumeGrams ?: return skipped(entity = "DiaryDay", field = "volumeGrams")
        return DiaryDay(entryDate = entryDate, volumeGrams = volumeGrams)
    }

    private fun parseInstant(raw: String?): Instant? {
        if (raw == null) return null
        return runCatching { Instant.parse(raw) }.getOrNull()
    }

    private fun parseKind(raw: String?): ExerciseKind? = ExerciseKind.entries.firstOrNull { it.name == raw }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw == null) return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    private fun <T> skipped(entity: String, field: String): T? {
        logger.error(tag = LOG_TAG, message = "Пропущен $entity: в ответе нет или не разобрано поле $field")
        return null
    }
}
