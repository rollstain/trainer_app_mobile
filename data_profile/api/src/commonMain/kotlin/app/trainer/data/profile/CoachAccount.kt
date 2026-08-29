package app.trainer.data.profile

data class CoachAccount(
    val coachId: String,
    val displayName: String,
    val createdAtIso: String,
    val activeClients: Int,
    val isOwner: Boolean,
)

data class CoachAccountCard(
    val coachId: String,
    val displayName: String,
    val email: String?,
    val phone: String?,
    val login: String?,
    val zoneId: String,
    val createdAtIso: String,
    val activeClients: Int,
    val archivedClients: Int,
    val lastSeenAtIso: String?,
    val hasPassword: Boolean,
    val providers: List<String>,
    val isOwner: Boolean,
)
