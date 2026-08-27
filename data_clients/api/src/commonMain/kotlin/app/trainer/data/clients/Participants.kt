package app.trainer.data.clients

import kotlin.time.Instant
data class CoachSummary(
    val coachId: String,
    val userId: String,
    val displayName: String,
    val zoneId: String,
    val cancellationWindowHours: Int,
)

data class CoachClient(
    val userId: String,
    val displayName: String,
    val hasMedicalNotes: Boolean,
    val linkedAt: Instant?,
)

data class CoachPolicy(
    val cancellationWindowHours: Int,
    val reminderHour: Int,
    val sessionRemindersEnabled: Boolean,
    val diaryRemindersEnabled: Boolean,
    val checkInRemindersEnabled: Boolean,
)
