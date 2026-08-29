package app.trainer.feature.account.coachrequests.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.data.auth.CoachRequest
import app.trainer.data.auth.CoachRequestsRepository
import app.trainer.entities.RequestResult
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CoachRequestsScreenModel(
    private val requestsRepository: CoachRequestsRepository,
) : BaseScreenModel<CoachRequestsState, CoachRequestsSideEffect, CoachRequestsEvent>(
    initialState = CoachRequestsState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = requestsRepository.pending()) {
                is RequestResult.Error -> updateState { it.copy(isLoading = false, failure = loaded) }
                is RequestResult.Success -> updateState {
                    it.copy(
                        requests = loaded.data.map(::rowOf).toImmutableList(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    override fun dispatch(event: CoachRequestsEvent) {
        when (event) {
            CoachRequestsEvent.OnReloadRequested -> onFetchData()
            is CoachRequestsEvent.OnApproved -> decide(requestId = event.requestId, approve = true)
            is CoachRequestsEvent.OnDeclined -> decide(requestId = event.requestId, approve = false)
        }
    }

    private fun decide(requestId: String, approve: Boolean) {
        screenModelScope { current ->
            if (current.decidingId != null) return@screenModelScope
            updateState { it.copy(decidingId = requestId) }
            val decided = if (approve) {
                requestsRepository.approve(requestId)
            } else {
                requestsRepository.decline(requestId)
            }
            updateState { it.copy(decidingId = null) }
            when (decided) {
                is RequestResult.Error -> postSideEffect(CoachRequestsSideEffect.ShowFailure(decided))
                is RequestResult.Success -> {
                    if (approve) postSideEffect(CoachRequestsSideEffect.ShowApproved)
                    onFetchData()
                }
            }
        }
    }

    private fun rowOf(request: CoachRequest): CoachRequestRow = CoachRequestRow(
        requestId = request.id,
        displayName = request.displayName,
        askedAtLabel = dayMonthOf(
            Instant.parse(request.createdAtIso).toLocalDateTime(TimeZone.currentSystemDefault()).date
        ),
    )
}
