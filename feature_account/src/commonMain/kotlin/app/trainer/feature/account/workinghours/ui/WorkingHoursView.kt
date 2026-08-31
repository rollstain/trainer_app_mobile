package app.trainer.feature.account.workinghours.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.account.workinghours.mvi.WorkingHourIssue
import app.trainer.feature.account.workinghours.mvi.WorkingHourRow
import app.trainer.feature.account.workinghours.mvi.WorkingHoursEvent
import app.trainer.feature.account.workinghours.mvi.WorkingHoursState
import app.trainer.strings.Res
import app.trainer.strings.working_hours_apply_all
import app.trainer.strings.working_hours_day_off
import app.trainer.strings.working_hours_end_before_start
import app.trainer.strings.working_hours_first_hint
import app.trainer.strings.working_hours_from
import app.trainer.strings.working_hours_header
import app.trainer.strings.working_hours_incomplete
import app.trainer.strings.working_hours_leave_confirm
import app.trainer.strings.working_hours_leave_description
import app.trainer.strings.working_hours_leave_save
import app.trainer.strings.working_hours_leave_title
import app.trainer.strings.working_hours_none_left_description
import app.trainer.strings.working_hours_none_left_title
import app.trainer.strings.working_hours_not_set_description
import app.trainer.strings.working_hours_not_set_title
import app.trainer.strings.working_hours_retry
import app.trainer.strings.working_hours_save_days
import app.trainer.strings.working_hours_save_failed
import app.trainer.strings.working_hours_title
import app.trainer.strings.working_hours_to
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppSettingShimmerList
import app.trainer.uikit.widgets.AppSwitch
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.ConfirmDialogDismiss
import app.trainer.uikit.widgets.ConfirmDialogTone
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TextFieldValueTone
import app.trainer.uikit.widgets.TimeDigitsVisualTransformation
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_ROWS = 7
private val DAY_LABEL_WIDTH = 44.dp
private val ISSUE_MARK_SIZE = 18.dp
private const val TIME_PLACEHOLDER = "09:00"

@Composable
fun WorkingHoursView(
    modifier: Modifier = Modifier,
    state: WorkingHoursState,
    onEvent: (WorkingHoursEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground().navigationBarsPadding()) {
        AppTopBar(
            title = stringResource(Res.string.working_hours_title),
            leading = TopBarLeading.Back(onClick = { onEvent(WorkingHoursEvent.OnBackRequested) }),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(WorkingHoursEvent.OnReloadRequested) },
                )
                state.isLoading -> AppSettingShimmerList(count = SHIMMER_ROWS)
                else -> ScheduleContent(state = state, onEvent = onEvent)
            }
        }
        SavePanel(state = state, onEvent = onEvent)
    }
    if (state.isLeaveDialogVisible) {
        AppConfirmDialog(
            title = stringResource(Res.string.working_hours_leave_title),
            description = stringResource(Res.string.working_hours_leave_description),
            confirmText = stringResource(Res.string.working_hours_leave_confirm),
            onConfirm = { onEvent(WorkingHoursEvent.OnLeaveConfirmed) },
            onDismissRequest = { onEvent(WorkingHoursEvent.OnLeaveDialogDismissed) },
            tone = ConfirmDialogTone.Danger,
            dismiss = ConfirmDialogDismiss.Action(
                text = stringResource(Res.string.working_hours_leave_save),
                onClick = { onEvent(WorkingHoursEvent.OnSaveClicked) },
            ),
        )
    }
}

@Composable
private fun ScheduleContent(state: WorkingHoursState, onEvent: (WorkingHoursEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = AppTheme.spacing.dp12),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        if (state.isScheduleAbsent) {
            WarningCard(
                title = stringResource(Res.string.working_hours_not_set_title),
                description = stringResource(Res.string.working_hours_not_set_description),
            )
        } else {
            AppText(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16),
                text = stringResource(Res.string.working_hours_header),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
        }
        DayList(state = state, onEvent = onEvent)
        if (state.allDaysOff && state.isDirty && !state.isScheduleAbsent) {
            WarningCard(
                title = stringResource(Res.string.working_hours_none_left_title),
                description = stringResource(Res.string.working_hours_none_left_description),
            )
        }
        if (state.canApplyToAll) {
            AppButton(
                modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.dp16),
                text = stringResource(Res.string.working_hours_apply_all),
                onClick = { onEvent(WorkingHoursEvent.OnApplyToAllClicked) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Medium,
            )
        }
        if (state.isScheduleAbsent) {
            AppText(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16),
                text = stringResource(Res.string.working_hours_first_hint),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textMuted,
            )
        }
        Box(modifier = Modifier.height(AppTheme.spacing.dp16))
    }
}

@Composable
private fun WarningCard(title: String, description: String) {
    Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
        AppCard(
            background = AppTheme.colors.warningSoft,
            decoration = CardDecoration.Stripe(AppTheme.colors.warning),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
                AppText(
                    text = title,
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = description,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun DayList(state: WorkingHoursState, onEvent: (WorkingHoursEvent) -> Unit) {
    val firstWorkingDay = state.rows.firstOrNull { it.isWorking }?.dayOfWeek
    Column(modifier = Modifier.fillMaxWidth().background(AppTheme.colors.bgSurface)) {
        state.rows.forEachIndexed { index, row ->
            DayRow(
                row = row,
                showFieldLabels = row.dayOfWeek == firstWorkingDay,
                onEvent = onEvent,
            )
            if (index != state.rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTheme.borders.hairline)
                        .background(AppTheme.colors.border),
                )
            }
        }
    }
}

@Composable
private fun DayRow(row: WorkingHourRow, showFieldLabels: Boolean, onEvent: (WorkingHoursEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(if (row.isChanged) AppTheme.colors.accentSoft else Color.Transparent)
            .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = AppTheme.sizing.fieldHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        ) {
            AppText(
                modifier = Modifier.width(DAY_LABEL_WIDTH),
                text = row.label,
                style = AppTheme.typography.bodyStrong,
                color = if (row.isWorking) AppTheme.colors.textPrimary else AppTheme.colors.textMuted,
            )
            if (row.isWorking) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                ) {
                    AppTextField(
                        modifier = Modifier.weight(1f),
                        value = row.opensText,
                        onValueChange = { onEvent(WorkingHoursEvent.OnOpensChanged(row.dayOfWeek, it)) },
                        kind = TextFieldKind.Numeric,
                        label = fieldLabel(showFieldLabels, Res.string.working_hours_from),
                        placeholder = TIME_PLACEHOLDER,
                        valueTone = if (row.isPrefilled) TextFieldValueTone.Muted else TextFieldValueTone.Regular,
                        valueTransformation = TimeDigitsVisualTransformation,
                    )
                    AppTextField(
                        modifier = Modifier.weight(1f),
                        value = row.closesText,
                        onValueChange = { onEvent(WorkingHoursEvent.OnClosesChanged(row.dayOfWeek, it)) },
                        kind = TextFieldKind.Numeric,
                        label = fieldLabel(showFieldLabels, Res.string.working_hours_to),
                        placeholder = TIME_PLACEHOLDER,
                        valueTone = if (row.isPrefilled) TextFieldValueTone.Muted else TextFieldValueTone.Regular,
                        valueTransformation = TimeDigitsVisualTransformation,
                    )
                }
            } else {
                AppText(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.working_hours_day_off),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textMuted,
                )
            }
            AppSwitch(
                checked = row.isWorking,
                onCheckedChange = { onEvent(WorkingHoursEvent.OnDayToggled(row.dayOfWeek)) },
            )
        }
        row.issue?.let { issue -> IssueNote(issue = issue) }
    }
}

@Composable
private fun fieldLabel(
    showFieldLabels: Boolean,
    resource: org.jetbrains.compose.resources.StringResource,
): TextFieldLabel = if (showFieldLabels) {
    TextFieldLabel.Text(stringResource(resource))
} else {
    TextFieldLabel.None
}

@Composable
private fun IssueNote(issue: WorkingHourIssue) {
    val tone = when (issue) {
        WorkingHourIssue.Incomplete -> AppTheme.colors.warning
        WorkingHourIssue.EndBeforeStart -> AppTheme.colors.danger
    }
    val text = when (issue) {
        WorkingHourIssue.Incomplete -> stringResource(Res.string.working_hours_incomplete)
        WorkingHourIssue.EndBeforeStart -> stringResource(Res.string.working_hours_end_before_start)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = DAY_LABEL_WIDTH + AppTheme.spacing.dp12),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(ISSUE_MARK_SIZE)
                .clip(CircleShape)
                .background(tone),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = "!", style = AppTheme.typography.caption, color = AppTheme.colors.bgSurface)
        }
        AppText(
            modifier = Modifier.weight(1f),
            text = text,
            style = AppTheme.typography.caption,
            color = tone,
        )
    }
}

@Composable
private fun SavePanel(state: WorkingHoursState, onEvent: (WorkingHoursEvent) -> Unit) {
    if (!state.isDirty && !state.isSaveFailed) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.bgSurface)
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        if (state.isSaveFailed) {
            AppText(
                text = stringResource(Res.string.working_hours_save_failed),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.warning,
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = if (state.isSaveFailed) {
                stringResource(Res.string.working_hours_retry)
            } else {
                pluralStringResource(Res.plurals.working_hours_save_days, state.changedDays, state.changedDays)
            },
            onClick = { onEvent(WorkingHoursEvent.OnSaveClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSaving -> ButtonState.Loading
                state.hasIssues -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
    }
}
