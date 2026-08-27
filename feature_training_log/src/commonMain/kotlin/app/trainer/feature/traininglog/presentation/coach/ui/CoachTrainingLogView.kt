package app.trainer.feature.traininglog.presentation.coach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.traininglog.presentation.coach.mvi.CoachTrainingLogEvent
import app.trainer.feature.traininglog.presentation.coach.mvi.CoachTrainingLogState
import app.trainer.feature.traininglog.presentation.coach.mvi.LoggedDayRow
import app.trainer.strings.Res
import app.trainer.strings.coach_training_log_empty_action
import app.trainer.strings.coach_training_log_empty_description
import app.trainer.strings.coach_training_log_empty_title
import app.trainer.strings.coach_training_log_next_period_action
import app.trainer.strings.coach_training_log_title
import app.trainer.strings.coach_training_log_volume_caption
import app.trainer.strings.coach_training_log_workouts_caption
import app.trainer.uikit.AppTheme
import app.trainer.uikit.leadingStripe
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TopBarAction
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.TopBarSubtitle
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 4

@Composable
fun CoachTrainingLogView(
    modifier: Modifier = Modifier,
    state: CoachTrainingLogState,
    onEvent: (CoachTrainingLogEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.coach_training_log_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
            subtitle = TopBarSubtitle.Text(state.periodLabel),
            action = TopBarAction.Icon(
                painter = { AppIcons.next },
                contentDescription = stringResource(Res.string.coach_training_log_next_period_action),
                onClick = { onEvent(CoachTrainingLogEvent.OnNextPeriodClicked) },
            ),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(CoachTrainingLogEvent.OnRetryClicked) },
                )
                state.isLoading -> AppCardShimmerList(
                    count = SHIMMER_CARDS,
                    lines = SHIMMER_CARD_LINES,
                )
                state.days.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.coach_training_log_empty_title),
                    description = stringResource(Res.string.coach_training_log_empty_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.coach_training_log_empty_action),
                        onClick = { onEvent(CoachTrainingLogEvent.OnPreviousPeriodClicked) },
                    ),
                )
                else -> DayList(state = state)
            }
        }
    }
}

@Composable
private fun DayList(state: CoachTrainingLogState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        item(key = "summary") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    caption = stringResource(Res.string.coach_training_log_workouts_caption),
                    value = state.totalWorkoutsLabel,
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    caption = stringResource(Res.string.coach_training_log_volume_caption),
                    value = state.totalVolumeLabel,
                )
            }
        }
        items(items = state.days, key = { it.entryId }) { day ->
            DayCard(day = day)
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, caption: String, value: String) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            AppText(
                text = caption,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            AppText(
                text = value,
                style = AppTheme.typography.numericBig,
                color = AppTheme.colors.textPrimary,
            )
        }
    }
}

@Composable
private fun DayCard(day: LoggedDayRow) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AppText(
                    text = day.dateLabel,
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = day.volumeLabel,
                    style = AppTheme.typography.numeric,
                    color = AppTheme.colors.textSecondary,
                )
            }
            day.exercises.forEach { exercise ->
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
                    AppText(
                        text = exercise.exerciseName,
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = exercise.setsLabel,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
            day.notes?.let { notes ->
                AppText(
                    modifier = Modifier
                        .leadingStripe(
                            color = AppTheme.colors.border,
                            width = AppTheme.borders.accentStripe,
                        )
                        .padding(start = AppTheme.spacing.dp12),
                    text = notes,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}
