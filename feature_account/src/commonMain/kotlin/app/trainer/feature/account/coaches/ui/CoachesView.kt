package app.trainer.feature.account.coaches.ui

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
import app.trainer.feature.account.coaches.mvi.CoachRow
import app.trainer.feature.account.coaches.mvi.CoachesEvent
import app.trainer.feature.account.coaches.mvi.CoachesState
import app.trainer.strings.Res
import app.trainer.strings.coaches_clients
import app.trainer.strings.coaches_empty_description
import app.trainer.strings.coaches_empty_title
import app.trainer.strings.coaches_owner_mark
import app.trainer.strings.coaches_title
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppCellShimmerList
import app.trainer.uikit.widgets.AppListCell
import app.trainer.uikit.widgets.AppSectionHeader
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ListCellPreview
import app.trainer.uikit.widgets.ListCellTrailing
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.SectionCount
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CELLS = 4
private const val LOAD_MORE_CELLS = 2

@Composable
fun CoachesView(
    modifier: Modifier = Modifier,
    state: CoachesState,
    onEvent: (CoachesEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.coaches_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(CoachesEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCellShimmerList(count = SHIMMER_CELLS)
                state.coaches.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.coaches_empty_title),
                    description = stringResource(Res.string.coaches_empty_description),
                    action = PlaceholderAction.None,
                )
                else -> CoachList(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun CoachList(state: CoachesState, onEvent: (CoachesEvent) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            AppSectionHeader(
                title = stringResource(Res.string.coaches_title),
                count = if (state.hasMore) SectionCount.None else SectionCount.Value(state.coaches.size),
            )
        }
        items(items = state.coaches, key = { it.coachId }) { coach ->
            AppListCell(
                title = coach.displayName,
                onClick = { onEvent(CoachesEvent.OnCoachClicked(coach.coachId)) },
                preview = ListCellPreview.Text(previewOf(coach)),
                trailing = ListCellTrailing.Time(coach.joinedLabel),
            )
        }
        if (state.hasMore) {
            item(key = "load-more") {
                LaunchedEffect(state.nextCursor) { onEvent(CoachesEvent.OnEndReached) }
                AppCellShimmerList(count = LOAD_MORE_CELLS)
            }
        }
    }
}

@Composable
private fun previewOf(coach: CoachRow): String {
    val clients = pluralStringResource(Res.plurals.coaches_clients, coach.activeClients, coach.activeClients)
    return if (coach.isOwner) "${stringResource(Res.string.coaches_owner_mark)} · $clients" else clients
}
