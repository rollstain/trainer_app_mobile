package app.trainer.feature.progress.presentation.checkin.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.progress.presentation.checkin.mvi.AwaitingCheckInRow
import app.trainer.feature.progress.presentation.checkin.mvi.CoachCheckInsEvent
import app.trainer.feature.progress.presentation.checkin.mvi.CoachCheckInsState
import app.trainer.strings.Res
import app.trainer.strings.check_ins_coach_empty_description
import app.trainer.strings.check_ins_coach_empty_title
import app.trainer.strings.check_ins_coach_title
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppCellShimmerList
import app.trainer.uikit.widgets.AppListCell
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ListCellSize
import app.trainer.uikit.widgets.ListCellTrailing
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CELLS = 6
private const val LOAD_MORE_CELLS = 2

@Composable
fun CoachCheckInsView(
    state: CoachCheckInsState,
    onEvent: (CoachCheckInsEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.check_ins_coach_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(CoachCheckInsEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCellShimmerList(count = SHIMMER_CELLS)
                state.checkIns.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.check_ins_coach_empty_title),
                    description = stringResource(Res.string.check_ins_coach_empty_description),
                    action = PlaceholderAction.None,
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.checkIns, key = AwaitingCheckInRow::checkInId) { row ->
                        AppListCell(
                            modifier = Modifier.animateItem(),
                            title = row.clientDisplayName,
                            onClick = { onEvent(CoachCheckInsEvent.OnCheckInClicked(row.clientUserId)) },
                            size = ListCellSize.Small,
                            trailing = ListCellTrailing.Time(value = row.dateLabel),
                        )
                    }
                    if (state.hasMore) {
                        item(key = "load-more") {
                            LaunchedEffect(state.nextCursor) { onEvent(CoachCheckInsEvent.OnEndReached) }
                            AppCellShimmerList(count = LOAD_MORE_CELLS)
                        }
                    }
                }
            }
        }
    }
}
