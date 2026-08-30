package app.trainer.feature.progress.presentation.checkin.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.data.progress.AwaitingCheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.entities.RequestResult

private const val PAGE_SIZE = 20

class CoachCheckInsScreenModel(
    private val checkInRepository: CheckInRepository,
) : BaseScreenModel<CoachCheckInsState, CoachCheckInsSideEffect, CoachCheckInsEvent>(
    initialState = CoachCheckInsState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = checkInRepository.awaitingReview(limit = PAGE_SIZE, after = null)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
                    postSideEffect(CoachCheckInsSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> updateState {
                    it.withFirstPage(
                        rows = loaded.data.items.map(::toRow),
                        nextCursor = loaded.data.nextCursor,
                    )
                }
            }
        }
    }

    override fun dispatch(event: CoachCheckInsEvent) {
        when (event) {
            CoachCheckInsEvent.OnReloadRequested -> onFetchData()
            CoachCheckInsEvent.OnEndReached -> loadMore()
            is CoachCheckInsEvent.OnCheckInClicked -> screenModelScope {
                postSideEffect(CoachCheckInsSideEffect.OpenClientCard(event.clientUserId))
            }
        }
    }

    private fun loadMore() {
        screenModelScope { state ->
            val cursor = state.nextCursor ?: return@screenModelScope
            if (state.isLoadingMore) return@screenModelScope
            updateState { it.copy(isLoadingMore = true) }
            when (val page = checkInRepository.awaitingReview(limit = PAGE_SIZE, after = cursor)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingMore = false) }
                    postSideEffect(CoachCheckInsSideEffect.ShowFailure(page))
                }
                is RequestResult.Success -> updateState {
                    it.withNextPage(rows = page.data.items.map(::toRow), nextCursor = page.data.nextCursor)
                }
            }
        }
    }

    private fun toRow(checkIn: AwaitingCheckIn): AwaitingCheckInRow = AwaitingCheckInRow(
        checkInId = checkIn.checkInId,
        clientUserId = checkIn.clientUserId,
        clientDisplayName = checkIn.clientDisplayName,
        dateLabel = dayMonthOf(checkIn.checkInDate),
    )
}
