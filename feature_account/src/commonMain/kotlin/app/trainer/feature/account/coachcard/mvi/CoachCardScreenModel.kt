package app.trainer.feature.account.coachcard.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthYearOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.data.profile.CoachAccountCard
import app.trainer.data.profile.OwnerRepository
import app.trainer.entities.RequestResult
import app.trainer.feature.account.providers.providerNameOf
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CoachCardScreenModel(
    private val coachId: String,
    private val ownerRepository: OwnerRepository,
) : BaseScreenModel<CoachCardState, Nothing, CoachCardEvent>(
    initialState = CoachCardState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = ownerRepository.coach(coachId)) {
                is RequestResult.Error -> updateState { it.copy(isLoading = false, failure = loaded) }
                is RequestResult.Success -> updateState { stateOf(loaded.data) }
            }
        }
    }

    override fun dispatch(event: CoachCardEvent) {
        when (event) {
            CoachCardEvent.OnReloadRequested -> onFetchData()
        }
    }

    private fun stateOf(card: CoachAccountCard): CoachCardState = CoachCardState(
        displayName = card.displayName,
        isOwner = card.isOwner,
        joinedLabel = dayOf(card.createdAtIso),
        zoneId = card.zoneId,
        lastSeenLabel = card.lastSeenAtIso?.let(::momentOf),
        activeClients = card.activeClients,
        archivedClients = card.archivedClients,
        email = card.email,
        phone = card.phone,
        login = card.login,
        hasPassword = card.hasPassword,
        providers = card.providers.map(::providerNameOf).toImmutableList(),
        isLoading = false,
        failure = null,
    )

    private fun dayOf(iso: String): String =
        dayMonthYearOf(Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault()).date)

    private fun momentOf(iso: String): String {
        val local = Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dayMonthYearOf(local.date)} ${timeOfDayOf(local)}"
    }
}
