package app.trainer.data.progress

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

data class CheckInPhoto(
    val id: String,
    val downloadUrl: String,
    val originalName: String,
)

data class PreparedPhotoUpload(
    val photoId: String,
    val uploadUrl: String,
    val downloadUrl: String,
)

data class CheckIn(
    val id: String,
    val clientUserId: String,
    val checkInDate: LocalDate,
    val weightGrams: Int?,
    val waistMillimeters: Int?,
    val chestMillimeters: Int?,
    val hipsMillimeters: Int?,
    val wellbeing: Int?,
    val sleepQuality: Int?,
    val adherence: Int?,
    val notes: String?,
    val coachComment: String?,
    val isReviewed: Boolean,
    val photos: List<CheckInPhoto>,
)

data class CheckInDraft(
    val weightGrams: Int?,
    val waistMillimeters: Int?,
    val chestMillimeters: Int?,
    val hipsMillimeters: Int?,
    val wellbeing: Int?,
    val sleepQuality: Int?,
    val adherence: Int?,
    val notes: String?,
    val photoIds: List<String>,
)

data class AwaitingCheckIn(
    val checkInId: String,
    val clientUserId: String,
    val clientDisplayName: String,
    val checkInDate: LocalDate,
)

data class Habit(
    val id: String,
    val clientUserId: String,
    val title: String,
    val isSetByCoach: Boolean,
    val doneDates: List<LocalDate>,
)

data class FormCheck(
    val id: String,
    val clientUserId: String,
    val clientDisplayName: String,
    val exerciseId: String?,
    val exerciseName: String?,
    val videoUrl: String?,
    val note: String?,
    val coachComment: String?,
    val isReviewed: Boolean,
    val createdAt: Instant,
)
