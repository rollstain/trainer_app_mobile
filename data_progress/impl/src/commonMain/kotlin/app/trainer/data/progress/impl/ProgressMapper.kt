package app.trainer.data.progress.impl

import app.trainer.data.progress.AwaitingCheckIn
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInPhoto
import app.trainer.data.progress.FormCheck
import app.trainer.data.progress.Habit
import app.trainer.logger.Logger
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

private const val LOG_TAG = "progress-mapper"

class ProgressMapper(private val logger: Logger) {

    fun toAwaitingCheckIn(response: AwaitingCheckInResponse): AwaitingCheckIn? {
        val checkInId = response.checkInId ?: return null
        val clientUserId = response.clientUserId ?: return null
        val name = response.clientDisplayName ?: return null
        val date = response.checkInDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        return AwaitingCheckIn(
            checkInId = checkInId,
            clientUserId = clientUserId,
            clientDisplayName = name,
            checkInDate = date,
        )
    }

    fun toCheckIn(response: CheckInResponse): CheckIn? {
        val id = response.id ?: return skipped(entity = "CheckIn", field = "id")
        val clientUserId = response.clientUserId ?: return skipped(entity = "CheckIn", field = "clientUserId")
        val checkInDate = parseDate(response.checkInDate)
            ?: return skipped(entity = "CheckIn", field = "checkInDate")
        return CheckIn(
            id = id,
            clientUserId = clientUserId,
            checkInDate = checkInDate,
            weightGrams = response.weightGrams,
            waistMillimeters = response.waistMillimeters,
            chestMillimeters = response.chestMillimeters,
            hipsMillimeters = response.hipsMillimeters,
            wellbeing = response.wellbeing,
            sleepQuality = response.sleepQuality,
            adherence = response.adherence,
            notes = response.notes,
            coachComment = response.coachComment,
            isReviewed = response.isReviewed == true,
            photos = response.photos.orEmpty().mapNotNull(::toPhoto),
        )
    }

    private fun toPhoto(response: MediaFileResponse): CheckInPhoto? {
        val id = response.id ?: return skipped(entity = "CheckInPhoto", field = "id")
        val downloadUrl = response.downloadUrl
            ?: return skipped(entity = "CheckInPhoto", field = "downloadUrl")
        val originalName = response.originalName
            ?: return skipped(entity = "CheckInPhoto", field = "originalName")
        return CheckInPhoto(id = id, downloadUrl = downloadUrl, originalName = originalName)
    }

    fun toHabit(response: HabitResponse): Habit? {
        val id = response.id ?: return skipped(entity = "Habit", field = "id")
        val clientUserId = response.clientUserId ?: return skipped(entity = "Habit", field = "clientUserId")
        val title = response.title ?: return skipped(entity = "Habit", field = "title")
        val isSetByCoach = response.isSetByCoach ?: return skipped(entity = "Habit", field = "isSetByCoach")
        return Habit(
            id = id,
            clientUserId = clientUserId,
            title = title,
            isSetByCoach = isSetByCoach,
            doneDates = response.doneDates.orEmpty().mapNotNull(::parseDate),
        )
    }

    fun toFormCheck(response: FormCheckResponse): FormCheck? {
        val id = response.id ?: return skipped(entity = "FormCheck", field = "id")
        val clientUserId = response.clientUserId ?: return skipped(entity = "FormCheck", field = "clientUserId")
        val isReviewed = response.isReviewed ?: return skipped(entity = "FormCheck", field = "isReviewed")
        val createdAt = response.createdAt
            ?.let { raw -> runCatching { Instant.parse(raw) }.getOrNull() }
            ?: return skipped(entity = "FormCheck", field = "createdAt")
        return FormCheck(
            id = id,
            clientUserId = clientUserId,
            clientDisplayName = response.clientDisplayName.orEmpty(),
            exerciseId = response.exerciseId,
            exerciseName = response.exerciseName,
            videoUrl = response.video?.downloadUrl,
            note = response.note,
            coachComment = response.coachComment,
            isReviewed = isReviewed,
            createdAt = createdAt,
        )
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw == null) return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    private fun <T> skipped(entity: String, field: String): T? {
        logger.error(tag = LOG_TAG, message = "Пропущен $entity: в ответе нет или не разобрано поле $field")
        return null
    }
}
