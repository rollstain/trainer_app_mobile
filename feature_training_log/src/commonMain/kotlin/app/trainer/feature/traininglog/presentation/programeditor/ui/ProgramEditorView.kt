package app.trainer.feature.traininglog.presentation.programeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.traininglog.presentation.programeditor.mvi.DayContent
import app.trainer.feature.traininglog.presentation.programeditor.mvi.DayRow
import app.trainer.feature.traininglog.presentation.programeditor.mvi.ProgramEditorEvent
import app.trainer.feature.traininglog.presentation.programeditor.mvi.ProgramEditorState
import app.trainer.strings.Res
import app.trainer.strings.program_day_empty
import app.trainer.strings.program_week_chip
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardAction
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 4
private const val SHIMMER_CARD_LINES = 2

@Composable
fun ProgramEditorView(
    state: ProgramEditorState,
    onEvent: (ProgramEditorEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = state.title, leading = TopBarLeading.Back(onClick = onBackClick))
        if (state.weeksCount > 1) {
            WeekStrip(state = state, onEvent = onEvent)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ProgramEditorEvent.OnRetryClicked) },
                )
                else -> DayList(days = state.days, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun WeekStrip(state: ProgramEditorState, onEvent: (ProgramEditorEvent) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        items(items = (1..state.weeksCount).toList(), key = { it }) { week ->
            AppButton(
                text = stringResource(Res.string.program_week_chip, week),
                onClick = { onEvent(ProgramEditorEvent.OnWeekSelected(week)) },
                tone = if (week == state.selectedWeek) ButtonTone.Primary else ButtonTone.Secondary,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun DayList(days: List<DayRow>, onEvent: (ProgramEditorEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        items(items = days, key = { it.dayOfWeek }) { day ->
            AppCard(
                action = CardAction.Click(onClick = { onEvent(ProgramEditorEvent.OnDayClicked(day.dayOfWeek)) }),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        text = day.label,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textSecondary,
                    )
                    when (val content = day.content) {
                        DayContent.Empty -> AppText(
                            modifier = Modifier.weight(1f),
                            text = stringResource(Res.string.program_day_empty),
                            style = AppTheme.typography.body,
                            color = AppTheme.colors.textMuted,
                        )
                        is DayContent.Filled -> Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = content.title,
                                style = AppTheme.typography.bodyStrong,
                                color = AppTheme.colors.textPrimary,
                            )
                            AppText(
                                text = content.summary,
                                style = AppTheme.typography.numeric,
                                color = AppTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
