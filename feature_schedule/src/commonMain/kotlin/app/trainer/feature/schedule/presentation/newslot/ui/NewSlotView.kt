package app.trainer.feature.schedule.presentation.newslot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.schedule.presentation.newslot.mvi.NewSlotEvent
import app.trainer.feature.schedule.presentation.newslot.mvi.NewSlotState
import app.trainer.feature.schedule.presentation.newslot.mvi.SlotMode
import app.trainer.strings.Res
import app.trainer.strings.new_slot_days_title
import app.trainer.strings.new_slot_duration_title
import app.trainer.strings.new_slot_series_mode
import app.trainer.strings.new_slot_single_mode
import app.trainer.strings.new_slot_submit_series
import app.trainer.strings.new_slot_submit_single
import app.trainer.strings.new_slot_summary_title
import app.trainer.strings.new_slot_time_label
import app.trainer.strings.new_slot_title
import app.trainer.strings.new_slot_weeks_label
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val TIME_PLACEHOLDER = "19:00"

private val DURATION_OPTIONS = listOf(45, 60, 90)

@Composable
fun NewSlotView(
    modifier: Modifier = Modifier,
    state: NewSlotState,
    onEvent: (NewSlotEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground().navigationBarsPadding()) {
        AppTopBar(
            title = stringResource(Res.string.new_slot_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.dp16),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                AppButton(
                    text = stringResource(Res.string.new_slot_single_mode),
                    onClick = { onEvent(NewSlotEvent.OnModeChanged(SlotMode.Single)) },
                    tone = if (state.mode == SlotMode.Single) {
                        ButtonTone.Primary
                    } else {
                        ButtonTone.Secondary
                    },
                )
                AppButton(
                    text = stringResource(Res.string.new_slot_series_mode),
                    onClick = { onEvent(NewSlotEvent.OnModeChanged(SlotMode.Series)) },
                    tone = if (state.mode == SlotMode.Series) {
                        ButtonTone.Primary
                    } else {
                        ButtonTone.Secondary
                    },
                )
            }
            AppText(
                text = state.dateLabel,
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
            )
            AppTextField(
                value = state.timeText,
                onValueChange = { onEvent(NewSlotEvent.OnTimeChanged(it)) },
                kind = TextFieldKind.Numeric,
                label = TextFieldLabel.Text(stringResource(Res.string.new_slot_time_label)),
                placeholder = TIME_PLACEHOLDER,
            )
            DurationRow(state = state, onEvent = onEvent)
            if (state.mode == SlotMode.Series) {
                SeriesFields(state = state, onEvent = onEvent)
            }
            SummaryCard(summary = state.summaryLabel)
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = if (state.mode == SlotMode.Single) {
                stringResource(Res.string.new_slot_submit_single)
            } else {
                stringResource(Res.string.new_slot_submit_series)
            },
            onClick = { onEvent(NewSlotEvent.OnSubmitClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSubmitting -> ButtonState.Loading
                !state.isSubmitEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
    }
}

@Composable
private fun DurationRow(state: NewSlotState, onEvent: (NewSlotEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
        AppText(
            text = stringResource(Res.string.new_slot_duration_title),
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            DURATION_OPTIONS.forEach { minutes ->
                AppButton(
                    text = "$minutes",
                    onClick = { onEvent(NewSlotEvent.OnDurationChanged(minutes)) },
                    tone = if (state.durationMinutes == minutes) {
                        ButtonTone.Primary
                    } else {
                        ButtonTone.Secondary
                    },
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun SeriesFields(state: NewSlotState, onEvent: (NewSlotEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
        AppText(
            text = stringResource(Res.string.new_slot_days_title),
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            state.weekDays.forEach { day ->
                AppButton(
                    text = day.label,
                    onClick = { onEvent(NewSlotEvent.OnWeekDayToggled(day.dayOfWeek)) },
                    tone = if (day.isSelected) ButtonTone.Primary else ButtonTone.Secondary,
                    size = ButtonSize.Small,
                )
            }
        }
        AppTextField(
            value = state.weeksCount.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let { onEvent(NewSlotEvent.OnWeeksCountChanged(it)) }
            },
            kind = TextFieldKind.Numeric,
            label = TextFieldLabel.Text(stringResource(Res.string.new_slot_weeks_label)),
        )
    }
}

@Composable
private fun SummaryCard(summary: String) {
    if (summary.isEmpty()) return
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            AppText(
                text = stringResource(Res.string.new_slot_summary_title),
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
            )
            AppText(
                text = summary,
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
            )
        }
    }
}
