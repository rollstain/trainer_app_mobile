package app.trainer.data.clients

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
)
