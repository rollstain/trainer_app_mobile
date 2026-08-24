package app.trainer.feature.traininglog.presentation.coach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.feature.traininglog.presentation.coach.mvi.CoachTrainingLogEvent
import app.trainer.feature.traininglog.presentation.coach.mvi.CoachTrainingLogState
import app.trainer.feature.traininglog.presentation.coach.mvi.LoggedDayRow
import app.trainer.uikit.AppTheme
import app.trainer.uikit.leadingStripe
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppIconButton
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TopBarAction
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.TopBarSubtitle

private const val TITLE = "Дневник"
private const val WORKOUTS_CAPTION = "Тренировок"
private const val VOLUME_CAPTION = "Объём"
private const val EMPTY_TITLE = "За эту неделю записей нет"
private const val EMPTY_DESCRIPTION =
    "Возможно, человек тренировался без дневника. Спросите в чате или выберите другой период."
private const val EMPTY_ACTION = "Выбрать период"
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"

@Composable
fun CoachTrainingLogView(
    modifier: Modifier = Modifier,
    state: CoachTrainingLogState,
    onEvent: (CoachTrainingLogEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = TITLE,
            leading = TopBarLeading.Back(onClick = onBackClick),
            subtitle = TopBarSubtitle.Text(state.periodLabel),
            action = TopBarAction.Content(
                onClick = { onEvent(CoachTrainingLogEvent.OnNextPeriodClicked) },
                render = {
                    AppIconButton(
                        painter = AppIcons.next,
                        contentDescription = "Следующий период",
                        onClick = { onEvent(CoachTrainingLogEvent.OnNextPeriodClicked) },
                    )
                },
            ),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isFailed -> AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(CoachTrainingLogEvent.OnRetryClicked) },
                    ),
                )
                state.days.isEmpty() && !state.isLoading -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EMPTY_TITLE,
                    description = EMPTY_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = EMPTY_ACTION,
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
                    caption = WORKOUTS_CAPTION,
                    value = state.totalWorkoutsLabel,
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    caption = VOLUME_CAPTION,
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
    Column(
        modifier = modifier
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
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

@Composable
private fun DayCard(day: LoggedDayRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
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
