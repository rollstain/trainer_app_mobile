package app.trainer.data.profile

data class UserProfile(
    val userId: String,
    val displayName: String,
    val phone: String?,
    val email: String?,
    val login: String?,
    val hasPassword: Boolean,
    val passwordUpdatedAtIso: String?,
    val coachId: String?,
    val zoneId: String?,
    val hasCoach: Boolean,
    val isOwner: Boolean,
)
