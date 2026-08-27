package app.trainer.feature.clientcard.presentation.diaries.mvi

import app.trainer.base.diary.DiaryLapse
import app.trainer.entities.RequestResult
import app.trainer.uikit.widgets.ComplianceCell
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DiaryRow(
    val userId: String,
    val displayName: String,
    val lapse: DiaryLapse,
    val cells: ImmutableList<ComplianceCell>,
    val summaryLabel: String,
)

data class DiariesState(
    val lapsed: ImmutableList<DiaryRow>,
    val others: ImmutableList<DiaryRow>,
    val windowLabel: String,
    val thresholdDays: Int,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val isEmpty: Boolean get() = lapsed.isEmpty() && others.isEmpty()

    val isEveryoneLapsed: Boolean get() = lapsed.isNotEmpty() && others.isEmpty()

    companion object {

        fun initial(): DiariesState = DiariesState(
            lapsed = persistentListOf(),
            others = persistentListOf(),
            windowLabel = "",
            thresholdDays = 0,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface DiariesEvent {

    data object OnRetryClicked : DiariesEvent

    data class OnPersonClicked(val userId: String) : DiariesEvent
}

sealed interface DiariesSideEffect {

    data class OpenDiary(val userId: String) : DiariesSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : DiariesSideEffect
}
