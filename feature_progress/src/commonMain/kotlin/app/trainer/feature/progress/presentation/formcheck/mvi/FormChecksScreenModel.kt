package app.trainer.feature.progress.presentation.formcheck.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.data.progress.FormCheck
import app.trainer.data.progress.FormCheckRepository
import app.trainer.entities.RequestResult
import app.trainer.media.PickedMedia
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val PAGE_SIZE = 20
private const val MAX_VIDEO_BYTES = 26_214_400
private const val BYTES_IN_MEGABYTE = 1_048_576

class FormChecksScreenModel(
    private val formCheckRepository: FormCheckRepository,
) : BaseScreenModel<FormChecksState, FormChecksSideEffect, FormChecksEvent>(
    initialState = FormChecksState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = formCheckRepository.ownFormChecks(limit = PAGE_SIZE, after = null)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
                    postSideEffect(FormChecksSideEffect.ShowFailure(loaded))
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

    override fun dispatch(event: FormChecksEvent) {
        when (event) {
            FormChecksEvent.OnReloadRequested -> onFetchData()
            FormChecksEvent.OnEndReached -> loadMore()
            FormChecksEvent.OnTooLargeVideoDismissed -> updateState { it.copy(tooLargeVideo = null) }
            is FormChecksEvent.OnVideoPicked -> send(event.video)
        }
    }

    private fun send(video: PickedMedia) {
        screenModelScope { state ->
            if (state.isSending) return@screenModelScope
            if (video.bytes.size > MAX_VIDEO_BYTES) {
                updateState {
                    it.copy(
                        tooLargeVideo = TooLargeVideo(
                            megabytes = video.bytes.size / BYTES_IN_MEGABYTE,
                            limitMegabytes = MAX_VIDEO_BYTES / BYTES_IN_MEGABYTE,
                        )
                    )
                }
                return@screenModelScope
            }
            updateState { it.copy(isSending = true) }
            val sent = formCheckRepository.submit(
                fileName = video.fileName,
                contentType = video.contentType,
                bytes = video.bytes,
                exerciseId = null,
                note = null,
            )
            updateState { it.copy(isSending = false) }
            when (sent) {
                is RequestResult.Error -> postSideEffect(FormChecksSideEffect.ShowFailure(sent))
                is RequestResult.Success -> {
                    postSideEffect(FormChecksSideEffect.ShowSent)
                    onFetchData()
                }
            }
        }
    }

    private fun loadMore() {
        screenModelScope { state ->
            val cursor = state.nextCursor ?: return@screenModelScope
            if (state.isLoadingMore) return@screenModelScope
            updateState { it.copy(isLoadingMore = true) }
            when (val page = formCheckRepository.ownFormChecks(limit = PAGE_SIZE, after = cursor)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingMore = false) }
                    postSideEffect(FormChecksSideEffect.ShowFailure(page))
                }
                is RequestResult.Success -> updateState {
                    it.withNextPage(rows = page.data.items.map(::toRow), nextCursor = page.data.nextCursor)
                }
            }
        }
    }

    private fun toRow(check: FormCheck): FormCheckRow = FormCheckRow(
        formCheckId = check.id,
        dateLabel = dayMonthOf(check.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date),
        note = check.note,
        videoUrl = check.videoUrl,
        answer = answerOf(check),
    )

    private fun answerOf(check: FormCheck): CoachAnswer {
        if (!check.isReviewed) return CoachAnswer.Awaiting
        val comment = check.coachComment?.takeIf { it.isNotBlank() }
        return if (comment == null) CoachAnswer.Approved else CoachAnswer.Comment(comment)
    }
}
