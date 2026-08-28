package app.trainer.data.profile

data class UserProfile(
    val userId: String,
    val displayName: String,
    val phone: String?,
    val email: String?,
    val coachId: String?,
    val zoneId: String?,
    val hasCoach: Boolean,
)
