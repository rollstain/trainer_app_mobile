package app.trainer.feature.clientcard.presentation.diaries.mvi

import app.trainer.base.diary.DiaryLapse
import app.trainer.entities.RequestResult
import app.trainer.uikit.widgets.ComplianceCell
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val SEARCH_SHOWN_FROM_CLIENTS = 8

data class DiaryRow(
    val userId: String,
    val displayName: String,
    val lapse: DiaryLapse,
    val cells: ImmutableList<ComplianceCell>,
    val summaryLabel: String,
)

data class DiariesState(
    val rows: ImmutableList<DiaryRow>,
    val search: String,
    val windowLabel: String,
    val thresholdDays: Int,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val isSearchable: Boolean get() = rows.size >= SEARCH_SHOWN_FROM_CLIENTS

    val isSearching: Boolean get() = search.isNotBlank()

    val lapsed: List<DiaryRow> get() = if (isSearching) emptyList() else found.filter { it.lapse.isLapsed() }

    val others: List<DiaryRow> get() = if (isSearching) found else found.filterNot { it.lapse.isLapsed() }

    val isEmpty: Boolean get() = found.isEmpty()

    val isEveryoneLapsed: Boolean get() = lapsed.isNotEmpty() && others.isEmpty()

    private val found: List<DiaryRow>
        get() = rows.filter { it.displayName.contains(search.trim(), ignoreCase = true) }

    companion object {

        fun initial(): DiariesState = DiariesState(
            rows = persistentListOf(),
            search = "",
            windowLabel = "",
            thresholdDays = 0,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface DiariesEvent {

    data object OnRetryClicked : DiariesEvent

    data class OnSearchChanged(val query: String) : DiariesEvent

    data class OnPersonClicked(val userId: String) : DiariesEvent
}

sealed interface DiariesSideEffect {

    data class OpenDiary(val userId: String) : DiariesSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : DiariesSideEffect
}

private fun DiaryLapse.isLapsed(): Boolean = when (this) {
    is DiaryLapse.Lapsed, DiaryLapse.NeverLogged -> true
    DiaryLapse.Logging, DiaryLapse.NotStartedYet -> false
}
