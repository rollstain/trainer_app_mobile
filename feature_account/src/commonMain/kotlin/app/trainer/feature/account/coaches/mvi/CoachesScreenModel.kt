package app.trainer.feature.account.coaches.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthYearOf
import app.trainer.data.profile.CoachAccount
import app.trainer.data.profile.OwnerRepository
import app.trainer.entities.RequestResult
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val PAGE_SIZE = 30

class CoachesScreenModel(
    private val ownerRepository: OwnerRepository,
) : BaseScreenModel<CoachesState, CoachesSideEffect, CoachesEvent>(
    initialState = CoachesState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val page = ownerRepository.coaches(limit = PAGE_SIZE, after = null)) {
                is RequestResult.Error -> updateState { it.copy(isLoading = false, failure = page) }
                is RequestResult.Success -> updateState {
                    it.withFirstPage(
                        rows = page.data.items.map(::rowOf),
                        nextCursor = page.data.nextCursor,
                    )
                }
            }
        }
    }

    override fun dispatch(event: CoachesEvent) {
        when (event) {
            CoachesEvent.OnReloadRequested -> onFetchData()
            CoachesEvent.OnEndReached -> loadMore()
            is CoachesEvent.OnCoachClicked -> screenModelScope {
                postSideEffect(CoachesSideEffect.OpenCoach(event.coachId))
            }
        }
    }

    private fun loadMore() {
        screenModelScope { state ->
            val cursor = state.nextCursor ?: return@screenModelScope
            if (state.isLoadingMore) return@screenModelScope
            updateState { it.copy(isLoadingMore = true) }
            when (val page = ownerRepository.coaches(limit = PAGE_SIZE, after = cursor)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingMore = false) }
                    postSideEffect(CoachesSideEffect.ShowFailure(page))
                }
                is RequestResult.Success -> updateState {
                    it.withNextPage(
                        rows = page.data.items.map(::rowOf),
                        nextCursor = page.data.nextCursor,
                    )
                }
            }
        }
    }

    private fun rowOf(coach: CoachAccount): CoachRow {
        val joined = Instant.parse(coach.createdAtIso).toLocalDateTime(TimeZone.currentSystemDefault())
        return CoachRow(
            coachId = coach.coachId,
            displayName = coach.displayName,
            joinedLabel = dayMonthYearOf(joined.date),
            activeClients = coach.activeClients,
            isOwner = coach.isOwner,
        )
    }
}
