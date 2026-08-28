package app.trainer.feature.progress.presentation.formcheck.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.data.progress.FormCheck
import app.trainer.data.progress.FormCheckRepository
import app.trainer.entities.RequestResult
import app.trainer.media.PickedMedia
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
            when (val loaded = formCheckRepository.ownFormChecks()) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
                    postSideEffect(FormChecksSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> show(loaded.data)
            }
        }
    }

    override fun dispatch(event: FormChecksEvent) {
        when (event) {
            FormChecksEvent.OnReloadRequested -> onFetchData()
            is FormChecksEvent.OnVideoPicked -> send(event.video)
        }
    }

    private fun send(video: PickedMedia) {
        screenModelScope { state ->
            if (state.isSending) return@screenModelScope
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

    private fun show(checks: List<FormCheck>) {
        val rows = checks.map(::toRow)
        updateState { it.copy(checks = rows.toImmutableList(), isLoading = false, failure = null) }
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
