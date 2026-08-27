package app.trainer.feature.progress.presentation.progress.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.progress.presentation.progress.mvi.CoachReply
import app.trainer.feature.progress.presentation.progress.mvi.HabitDay
import app.trainer.feature.progress.presentation.progress.mvi.HabitRow
import app.trainer.feature.progress.presentation.progress.mvi.MetricChart
import app.trainer.feature.progress.presentation.progress.mvi.ProgressEvent
import app.trainer.feature.progress.presentation.progress.mvi.ProgressState
import app.trainer.strings.Res
import app.trainer.strings.progress_add_habit_action
import app.trainer.strings.progress_archive_action
import app.trainer.strings.progress_check_in_action
import app.trainer.strings.progress_check_in_empty
import app.trainer.strings.progress_check_in_title
import app.trainer.strings.progress_check_in_update_action
import app.trainer.strings.progress_coach_habit_mark
import app.trainer.strings.progress_coach_reply_title
import app.trainer.strings.progress_day_done_description
import app.trainer.strings.progress_day_future_description
import app.trainer.strings.progress_day_not_done_description
import app.trainer.strings.progress_dynamics_title
import app.trainer.strings.progress_habits_empty
import app.trainer.strings.progress_habits_title
import app.trainer.strings.progress_new_habit_label
import app.trainer.strings.progress_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppLineChart
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 3

@Composable
fun ProgressView(
    modifier: Modifier = Modifier,
    state: ProgressState,
    onEvent: (ProgressEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = stringResource(Res.string.progress_title))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ProgressEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(
                    count = SHIMMER_CARDS,
                    lines = SHIMMER_CARD_LINES,
                )
                else -> ProgressContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ProgressContent(state: ProgressState, onEvent: (ProgressEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        item(key = "check-in") {
            CheckInCard(state = state, onEvent = onEvent)
        }
        state.selectedChart?.let { chart ->
            item(key = "dynamics") {
                DynamicsCard(charts = state.charts, chart = chart, onEvent = onEvent)
            }
        }
        item(key = "habits-title") {
            AppText(
                text = stringResource(Res.string.progress_habits_title),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
            )
        }
        if (state.habits.isEmpty()) {
            item(key = "habits-empty") {
                AppText(
                    text = stringResource(Res.string.progress_habits_empty),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        items(count = state.habits.size, key = { state.habits[it].habitId }) { index ->
            HabitCard(
                modifier = Modifier.animateItem(),
                habit = state.habits[index],
                onEvent = onEvent,
            )
        }
        item(key = "new-habit") {
            NewHabitRow(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun CheckInCard(state: ProgressState, onEvent: (ProgressEvent) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = stringResource(Res.string.progress_check_in_title),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                if (state.hasCheckIn) {
                    AppText(
                        text = state.checkInDateLabel,
                        style = AppTheme.typography.overline,
                        color = AppTheme.colors.textMuted,
                    )
                }
            }
            AppText(
                text = state.checkInSummary
                    .takeIf { state.hasCheckIn }
                    ?: stringResource(Res.string.progress_check_in_empty),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
            )
            when (val reply = state.coachReply) {
                CoachReply.None -> Unit
                is CoachReply.Text -> Column(
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
                ) {
                    AppText(
                        text = stringResource(Res.string.progress_coach_reply_title),
                        style = AppTheme.typography.overline,
                        color = AppTheme.colors.accent,
                    )
                    AppText(
                        text = reply.value,
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textPrimary,
                    )
                }
            }
            AppButton(
                text = if (state.hasCheckIn) {
                    stringResource(Res.string.progress_check_in_update_action)
                } else {
                    stringResource(Res.string.progress_check_in_action)
                },
                onClick = { onEvent(ProgressEvent.OnCheckInClicked) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun DynamicsCard(
    charts: List<MetricChart>,
    chart: MetricChart,
    onEvent: (ProgressEvent) -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = stringResource(Res.string.progress_dynamics_title),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        text = chart.latestLabel,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = chart.deltaLabel,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                items(items = charts, key = { it.metric.name }) { item ->
                    AppButton(
                        text = item.title,
                        onClick = { onEvent(ProgressEvent.OnMetricSelected(item.metric)) },
                        tone = if (item.metric == chart.metric) {
                            ButtonTone.Primary
                        } else {
                            ButtonTone.Secondary
                        },
                        size = ButtonSize.Small,
                    )
                }
            }
            AppLineChart(
                values = chart.values,
                maxLabel = chart.maxLabel,
                minLabel = chart.minLabel,
                rangeLabel = chart.rangeLabel,
            )
        }
    }
}

@Composable
private fun HabitCard(
    modifier: Modifier = Modifier,
    habit: HabitRow,
    onEvent: (ProgressEvent) -> Unit,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    modifier = Modifier.weight(1f),
                    text = habit.title,
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = if (habit.isSetByCoach) {
                        stringResource(Res.string.progress_coach_habit_mark)
                    } else {
                        habit.doneCountLabel
                    },
                    style = AppTheme.typography.overline,
                    color = AppTheme.colors.textMuted,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                habit.days.forEach { day ->
                    HabitDayCell(
                        day = day,
                        onClick = {
                            onEvent(
                                ProgressEvent.OnHabitDayToggled(
                                    habitId = habit.habitId,
                                    dateIso = day.dateIso,
                                )
                            )
                        },
                    )
                }
            }
            if (!habit.isSetByCoach) {
                AppButton(
                    text = stringResource(Res.string.progress_archive_action),
                    onClick = { onEvent(ProgressEvent.OnHabitArchived(habit.habitId)) },
                    tone = ButtonTone.Text,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun HabitDayCell(day: HabitDay, onClick: () -> Unit) {
    val description = "${day.weekdayLabel}, ${dayStateDescription(day)}"
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.dp8)
    val background = when {
        day.isDone -> colors.success
        day.isFuture -> colors.bgSurfaceSunken
        else -> colors.bgSurface
    }
    val content = when {
        day.isDone -> colors.accentOn
        day.isFuture -> colors.textMuted
        else -> colors.textSecondary
    }
    Box(
        modifier = Modifier
            .size(AppTheme.sizing.buttonMedium)
            .background(color = background, shape = shape)
            .border(
                width = if (day.isToday) AppTheme.borders.focus else AppTheme.borders.hairline,
                color = if (day.isToday) colors.accent else colors.border,
                shape = shape,
            )
            .clickable(
                enabled = !day.isFuture,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = day.weekdayLabel,
            style = AppTheme.typography.overline,
            color = content,
        )
    }
}

@Composable
private fun NewHabitRow(state: ProgressState, onEvent: (ProgressEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.Bottom,
    ) {
        AppTextField(
            modifier = Modifier.weight(1f),
            value = state.newHabitTitle,
            onValueChange = { onEvent(ProgressEvent.OnNewHabitTitleChanged(it)) },
            kind = TextFieldKind.Text,
            label = TextFieldLabel.Text(stringResource(Res.string.progress_new_habit_label)),
        )
        AppButton(
            text = stringResource(Res.string.progress_add_habit_action),
            onClick = { onEvent(ProgressEvent.OnHabitAdded) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Large,
            state = if (state.isAddHabitEnabled) ButtonState.Idle else ButtonState.Disabled,
        )
    }
}

@Composable
private fun dayStateDescription(day: HabitDay): String = when {
    day.isFuture -> stringResource(Res.string.progress_day_future_description)
    day.isDone -> stringResource(Res.string.progress_day_done_description)
    else -> stringResource(Res.string.progress_day_not_done_description)
}
