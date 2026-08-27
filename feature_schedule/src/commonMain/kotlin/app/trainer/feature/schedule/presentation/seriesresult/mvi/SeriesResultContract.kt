package app.trainer.feature.schedule.presentation.seriesresult.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SeriesResultState(
    val createdLabels: ImmutableList<String>,
    val skippedLabels: ImmutableList<String>,
    val isExpired: Boolean,
    val failure: RequestResult.Error?,
    val isLoading: Boolean,
) {

    companion object {

        fun initial(): SeriesResultState = SeriesResultState(
            createdLabels = persistentListOf(),
            skippedLabels = persistentListOf(),
            isExpired = false,
            failure = null,
            isLoading = true,
        )
    }
}

sealed interface SeriesResultEvent {

    data object OnReloadRequested : SeriesResultEvent
}

sealed interface SeriesResultSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : SeriesResultSideEffect
}
