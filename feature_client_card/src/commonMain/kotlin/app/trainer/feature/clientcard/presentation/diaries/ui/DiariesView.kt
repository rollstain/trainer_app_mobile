package app.trainer.feature.clientcard.presentation.diaries.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.trainer.base.diary.DiaryLapse
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.clientcard.presentation.diaries.mvi.DiariesEvent
import app.trainer.feature.clientcard.presentation.diaries.mvi.DiariesState
import app.trainer.feature.clientcard.presentation.diaries.mvi.DiaryRow
import app.trainer.strings.Res
import app.trainer.strings.diaries_all_lapsed
import app.trainer.strings.diaries_days_short
import app.trainer.strings.diaries_empty_description
import app.trainer.strings.diaries_empty_title
import app.trainer.strings.diaries_everyone_title
import app.trainer.strings.diaries_lapsed_title
import app.trainer.strings.diaries_never_logged
import app.trainer.strings.diaries_not_started_yet
import app.trainer.strings.diaries_others_title
import app.trainer.strings.diaries_threshold
import app.trainer.strings.diaries_title
import app.trainer.strings.people_search_clear
import app.trainer.strings.people_search_empty_description
import app.trainer.strings.people_search_empty_title
import app.trainer.strings.people_search_placeholder
import app.trainer.strings.people_search_reset
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppCellShimmerList
import app.trainer.uikit.widgets.AppComplianceStrip
import app.trainer.uikit.widgets.AppSearchField
import app.trainer.uikit.widgets.AppSectionHeader
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.SectionCount
import app.trainer.uikit.widgets.SectionSummary
import app.trainer.uikit.widgets.highlightedMatch
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_ROWS = 5

@Composable
fun DiariesView(
    modifier: Modifier = Modifier,
    state: DiariesState,
    onEvent: (DiariesEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = stringResource(Res.string.diaries_title))
        if (state.isSearchable) {
            AppSearchField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp8),
                value = state.search,
                placeholder = stringResource(Res.string.people_search_placeholder),
                onValueChange = { onEvent(DiariesEvent.OnSearchChanged(it)) },
                onClear = { onEvent(DiariesEvent.OnSearchChanged("")) },
                clearDescription = stringResource(Res.string.people_search_clear),
            )
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> AppCellShimmerList(count = SHIMMER_ROWS)
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(DiariesEvent.OnRetryClicked) },
                )
                state.isEmpty && state.isSearching -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.people_search_empty_title),
                    description = stringResource(Res.string.people_search_empty_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.people_search_reset),
                        onClick = { onEvent(DiariesEvent.OnSearchChanged("")) },
                    ),
                )
                state.isEmpty -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.diaries_empty_title),
                    description = stringResource(Res.string.diaries_empty_description),
                    action = PlaceholderAction.None,
                )
                else -> DiariesContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun DiariesContent(state: DiariesState, onEvent: (DiariesEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppTheme.spacing.dp24),
    ) {
        item(key = "window") {
            AppText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppTheme.spacing.dp16,
                        end = AppTheme.spacing.dp16,
                        top = AppTheme.spacing.dp12,
                    ),
                text = state.windowLabel,
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
                textAlign = TextAlign.End,
            )
        }
        if (state.lapsed.isNotEmpty()) {
            item(key = "lapsed-title") {
                AppSectionHeader(
                    title = stringResource(Res.string.diaries_lapsed_title),
                    count = SectionCount.Value(state.lapsed.size),
                    summary = SectionSummary.Text(
                        stringResource(Res.string.diaries_threshold, state.thresholdDays)
                    ),
                )
            }
            items(items = state.lapsed, key = { "lapsed-${it.userId}" }) { row ->
                DiaryCell(
                    row = row,
                    query = state.search,
                    onClick = { onEvent(DiariesEvent.OnPersonClicked(row.userId)) },
                )
            }
        }
        if (state.isEveryoneLapsed) {
            item(key = "all-lapsed") {
                AppText(
                    modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
                    text = stringResource(Res.string.diaries_all_lapsed),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textMuted,
                )
            }
        } else {
            item(key = "others-title") {
                AppSectionHeader(
                    title = when {
                        state.lapsed.isEmpty() -> stringResource(Res.string.diaries_everyone_title)
                        else -> stringResource(Res.string.diaries_others_title)
                    },
                    count = SectionCount.Value(state.others.size),
                )
            }
            items(items = state.others, key = { "other-${it.userId}" }) { row ->
                DiaryCell(
                    row = row,
                    query = state.search,
                    onClick = { onEvent(DiariesEvent.OnPersonClicked(row.userId)) },
                )
            }
        }
    }
}

@Composable
private fun DiaryCell(row: DiaryRow, query: String, onClick: () -> Unit) {
    Column(modifier = Modifier.background(AppTheme.colors.bgSurface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppTheme.sizing.personRowMinHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.Top,
        ) {
            AppAvatar(displayName = row.displayName, size = AvatarSize.Large)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        modifier = Modifier.weight(1f),
                        text = highlightedMatch(text = row.displayName, query = query),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ActivityMeta(row = row)
                }
                AppComplianceStrip(cells = row.cells)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.borders.hairline)
                .background(AppTheme.colors.border),
        )
    }
}

@Composable
private fun ActivityMeta(row: DiaryRow) {
    when (val lapse = row.lapse) {
        DiaryLapse.Logging -> AppText(
            text = row.summaryLabel,
            style = AppTheme.typography.numeric,
            color = AppTheme.colors.textSecondary,
        )
        is DiaryLapse.Lapsed -> Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
            verticalAlignment = Alignment.Bottom,
        ) {
            AppText(
                text = lapse.days.toString(),
                style = AppTheme.typography.numericBig,
                color = AppTheme.colors.warning,
            )
            AppText(
                text = stringResource(Res.string.diaries_days_short),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.warning,
            )
        }
        DiaryLapse.NeverLogged -> AppText(
            text = stringResource(Res.string.diaries_never_logged),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.warning,
        )
        DiaryLapse.NotStartedYet -> AppText(
            text = stringResource(Res.string.diaries_not_started_yet),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
    }
}
