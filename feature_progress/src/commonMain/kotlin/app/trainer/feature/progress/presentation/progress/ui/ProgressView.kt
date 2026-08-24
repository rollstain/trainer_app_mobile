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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.feature.progress.presentation.progress.mvi.HabitDay
import app.trainer.feature.progress.presentation.progress.mvi.HabitRow
import app.trainer.feature.progress.presentation.progress.mvi.ProgressEvent
import app.trainer.feature.progress.presentation.progress.mvi.ProgressState
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel

private const val TITLE = "Прогресс"
private const val CHECK_IN_TITLE = "Замеры и самочувствие"
private const val CHECK_IN_EMPTY = "Ещё не заполняли. Тренер увидит цифры сразу после сохранения."
private const val CHECK_IN_ACTION = "Заполнить"
private const val CHECK_IN_UPDATE_ACTION = "Обновить"
private const val HABITS_TITLE = "Привычки"
private const val HABITS_EMPTY = "Пока нет ни одной. Добавьте то, что хотите держать под контролем."
private const val NEW_HABIT_LABEL = "Новая привычка"
private const val ADD_HABIT_ACTION = "Добавить"
private const val ARCHIVE_ACTION = "Убрать"
private const val COACH_HABIT_MARK = "от тренера"
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"

@Composable
fun ProgressView(
    modifier: Modifier = Modifier,
    state: ProgressState,
    onEvent: (ProgressEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = TITLE)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (state.isFailed) {
                AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(ProgressEvent.OnRetryClicked) },
                    ),
                )
            } else {
                ProgressContent(state = state, onEvent = onEvent)
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
        item(key = "habits-title") {
            AppText(
                text = HABITS_TITLE,
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
            )
        }
        if (state.habits.isEmpty() && !state.isLoading) {
            item(key = "habits-empty") {
                AppText(
                    text = HABITS_EMPTY,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        items(count = state.habits.size, key = { state.habits[it].habitId }) { index ->
            HabitCard(habit = state.habits[index], onEvent = onEvent)
        }
        item(key = "new-habit") {
            NewHabitRow(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun CheckInCard(state: ProgressState, onEvent: (ProgressEvent) -> Unit) {
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = CHECK_IN_TITLE,
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
            text = if (state.hasCheckIn) state.checkInSummary else CHECK_IN_EMPTY,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        AppButton(
            text = if (state.hasCheckIn) CHECK_IN_UPDATE_ACTION else CHECK_IN_ACTION,
            onClick = { onEvent(ProgressEvent.OnCheckInClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Medium,
        )
    }
}

@Composable
private fun HabitCard(habit: HabitRow, onEvent: (ProgressEvent) -> Unit) {
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
                text = if (habit.isSetByCoach) COACH_HABIT_MARK else habit.doneCountLabel,
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
                text = ARCHIVE_ACTION,
                onClick = { onEvent(ProgressEvent.OnHabitArchived(habit.habitId)) },
                tone = ButtonTone.Text,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun HabitDayCell(day: HabitDay, onClick: () -> Unit) {
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
            .clickable(enabled = !day.isFuture, onClick = onClick),
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
            label = TextFieldLabel.Text(NEW_HABIT_LABEL),
        )
        AppButton(
            text = ADD_HABIT_ACTION,
            onClick = { onEvent(ProgressEvent.OnHabitAdded) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Large,
            state = if (state.isAddHabitEnabled) ButtonState.Idle else ButtonState.Disabled,
        )
    }
}
