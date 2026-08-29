package app.trainer.feature.account.coachcard.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CoachCardState(
    val displayName: String,
    val isOwner: Boolean,
    val joinedLabel: String,
    val zoneId: String,
    val lastSeenLabel: String?,
    val activeClients: Int,
    val archivedClients: Int,
    val email: String?,
    val phone: String?,
    val login: String?,
    val hasPassword: Boolean,
    val providers: ImmutableList<String>,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasSignInMethods: Boolean
        get() = hasPassword || providers.isNotEmpty()

    val hasContacts: Boolean
        get() = email != null || phone != null || login != null

    companion object {

        fun initial(): CoachCardState = CoachCardState(
            displayName = "",
            isOwner = false,
            joinedLabel = "",
            zoneId = "",
            lastSeenLabel = null,
            activeClients = 0,
            archivedClients = 0,
            email = null,
            phone = null,
            login = null,
            hasPassword = false,
            providers = persistentListOf(),
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface CoachCardEvent {

    data object OnReloadRequested : CoachCardEvent
}
