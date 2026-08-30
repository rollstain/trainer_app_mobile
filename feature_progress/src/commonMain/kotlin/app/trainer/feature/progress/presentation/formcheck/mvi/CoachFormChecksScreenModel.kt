package app.trainer.feature.progress.presentation.formcheck.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.data.progress.FormCheck
import app.trainer.data.progress.FormCheckRepository
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val PAGE_SIZE = 20

class CoachFormChecksScreenModel(
    private val formCheckRepository: FormCheckRepository,
) : BaseScreenModel<CoachFormChecksState, CoachFormChecksSideEffect, CoachFormChecksEvent>(
    initialState = CoachFormChecksState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = formCheckRepository.awaitingReview(limit = PAGE_SIZE, after = null)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
                    postSideEffect(CoachFormChecksSideEffect.ShowFailure(loaded))
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

    private fun loadMore() {
        screenModelScope { state ->
            val cursor = state.nextCursor ?: return@screenModelScope
            if (state.isLoadingMore) return@screenModelScope
            updateState { it.copy(isLoadingMore = true) }
            when (val page = formCheckRepository.awaitingReview(limit = PAGE_SIZE, after = cursor)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingMore = false) }
                    postSideEffect(CoachFormChecksSideEffect.ShowFailure(page))
                }
                is RequestResult.Success -> updateState {
                    it.withNextPage(rows = page.data.items.map(::toRow), nextCursor = page.data.nextCursor)
                }
            }
        }
    }

    override fun dispatch(event: CoachFormChecksEvent) {
        when (event) {
            CoachFormChecksEvent.OnReloadRequested -> onFetchData()
            CoachFormChecksEvent.OnEndReached -> loadMore()
            is CoachFormChecksEvent.OnDraftChanged -> updateDraft(
                formCheckId = event.formCheckId,
                text = event.text,
            )
            is CoachFormChecksEvent.OnReplyClicked -> reply(event.formCheckId)
        }
    }

    private fun updateDraft(formCheckId: String, text: String) {
        updateState { current ->
            current.copy(checks = current.checks.replacing(formCheckId) { it.copy(draft = text) })
        }
    }

    private fun reply(formCheckId: String) {
        screenModelScope { state ->
            val check = state.checks.firstOrNull { it.formCheckId == formCheckId } ?: return@screenModelScope
            if (check.isSending) return@screenModelScope
            updateState { current ->
                current.copy(checks = current.checks.replacing(formCheckId) { it.copy(isSending = true) })
            }
            val replied = formCheckRepository.review(
                formCheckId = formCheckId,
                comment = check.draft.trim().ifEmpty { null },
            )
            when (replied) {
                is RequestResult.Error -> {
                    updateState { current ->
                        current.copy(checks = current.checks.replacing(formCheckId) { it.copy(isSending = false) })
                    }
                    postSideEffect(CoachFormChecksSideEffect.ShowFailure(replied))
                }
                is RequestResult.Success -> {
                    updateState { current ->
                        current.copy(
                            checks = current.checks
                                .filterNot { it.formCheckId == formCheckId }
                                .toImmutableList(),
                        )
                    }
                    postSideEffect(CoachFormChecksSideEffect.ShowReplied)
                }
            }
        }
    }

    private fun toRow(check: FormCheck): AwaitingFormCheck = AwaitingFormCheck(
        formCheckId = check.id,
        clientDisplayName = check.clientDisplayName,
        dateLabel = dayMonthOf(check.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date),
        exerciseName = check.exerciseName,
        note = check.note,
        videoUrl = check.videoUrl,
        draft = "",
        isSending = false,
    )
}

private fun List<AwaitingFormCheck>.replacing(
    formCheckId: String,
    change: (AwaitingFormCheck) -> AwaitingFormCheck,
) = map { if (it.formCheckId == formCheckId) change(it) else it }.toImmutableList()
