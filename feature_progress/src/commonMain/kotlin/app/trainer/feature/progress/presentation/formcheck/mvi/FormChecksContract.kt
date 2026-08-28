package app.trainer.feature.progress.presentation.formcheck.mvi

import app.trainer.entities.RequestResult
import app.trainer.media.PickedMedia
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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

data class FormChecksState(
    val checks: ImmutableList<FormCheckRow>,
    val isLoading: Boolean,
    val isSending: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(): FormChecksState = FormChecksState(
            checks = persistentListOf(),
            isLoading = true,
            isSending = false,
            failure = null,
        )
    }
}

sealed interface FormChecksEvent {

    data object OnReloadRequested : FormChecksEvent

    data class OnVideoPicked(val video: PickedMedia) : FormChecksEvent
}

sealed interface FormChecksSideEffect {

    data object ShowSent : FormChecksSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : FormChecksSideEffect
}
