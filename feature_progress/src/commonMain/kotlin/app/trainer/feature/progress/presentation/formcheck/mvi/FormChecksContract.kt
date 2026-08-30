package app.trainer.feature.progress.presentation.formcheck.mvi

import app.trainer.entities.RequestResult
import app.trainer.media.PickedMedia
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

sealed interface CoachAnswer {

    data object Awaiting : CoachAnswer

    data object Approved : CoachAnswer

    data class Comment(val text: String) : CoachAnswer
}

data class FormCheckRow(
    val formCheckId: String,
    val dateLabel: String,
    val note: String?,
    val videoUrl: String?,
    val answer: CoachAnswer,
)

data class TooLargeVideo(val megabytes: Int, val limitMegabytes: Int)

data class FormChecksState(
    val checks: ImmutableList<FormCheckRow>,
    val tooLargeVideo: TooLargeVideo?,
    val nextCursor: String?,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val isSending: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasMore: Boolean
        get() = nextCursor != null

    fun withFirstPage(rows: List<FormCheckRow>, nextCursor: String?): FormChecksState = copy(
        checks = rows.toImmutableList(),
        nextCursor = nextCursor,
        isLoading = false,
        isLoadingMore = false,
        failure = null,
    )

    fun withNextPage(rows: List<FormCheckRow>, nextCursor: String?): FormChecksState {
        val known = checks.mapTo(mutableSetOf(), FormCheckRow::formCheckId)
        return copy(
            checks = (checks + rows.filterNot { it.formCheckId in known }).toImmutableList(),
            nextCursor = nextCursor,
            isLoadingMore = false,
        )
    }

    companion object {

        fun initial(): FormChecksState = FormChecksState(
            checks = persistentListOf(),
            tooLargeVideo = null,
            nextCursor = null,
            isLoading = true,
            isLoadingMore = false,
            isSending = false,
            failure = null,
        )
    }
}

sealed interface FormChecksEvent {

    data object OnReloadRequested : FormChecksEvent

    data object OnEndReached : FormChecksEvent

    data class OnVideoPicked(val video: PickedMedia) : FormChecksEvent

    data object OnTooLargeVideoDismissed : FormChecksEvent
}

sealed interface FormChecksSideEffect {

    data object ShowSent : FormChecksSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : FormChecksSideEffect
}
