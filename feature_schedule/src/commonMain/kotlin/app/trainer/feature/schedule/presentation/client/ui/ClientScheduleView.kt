package app.trainer.feature.schedule.presentation.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleDay
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleEvent
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleState
import app.trainer.feature.schedule.presentation.client.mvi.ClientSlotRow
import app.trainer.strings.Res
import app.trainer.strings.client_schedule_cancel_dialog_confirm
import app.trainer.strings.client_schedule_cancel_dialog_description
import app.trainer.strings.client_schedule_cancel_dialog_title
import app.trainer.strings.client_schedule_day_empty_description
import app.trainer.strings.client_schedule_day_empty_title
import app.trainer.strings.client_schedule_empty_action
import app.trainer.strings.client_schedule_empty_description
import app.trainer.strings.client_schedule_empty_title
import app.trainer.strings.client_schedule_next_week_action
import app.trainer.strings.client_schedule_no_coach_action
import app.trainer.strings.client_schedule_no_coach_description
import app.trainer.strings.client_schedule_no_coach_title
import app.trainer.strings.client_schedule_previous_week_action
import app.trainer.strings.client_schedule_privacy_note
import app.trainer.strings.client_schedule_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppClientSlotCard
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppSlotShimmerList
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AppWeekStrip
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.ClientSlotAction
import app.trainer.uikit.widgets.ClientSlotAvailability
import app.trainer.uikit.widgets.ConfirmDialogTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.SlotRequestView
import app.trainer.uikit.widgets.TopBarAction
import app.trainer.uikit.widgets.WeekDay
import app.trainer.uikit.widgets.WeekDayState
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_SLOTS = 4

@Composable
fun ClientScheduleView(
    modifier: Modifier = Modifier,
    state: ClientScheduleState,
    onEvent: (ClientScheduleEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        val isWeekLoaded = state.weekStart != null
        AppTopBar(
            title = state.weekTitle.ifEmpty { stringResource(Res.string.client_schedule_title) },
            secondaryAction = if (isWeekLoaded) {
                TopBarAction.Icon(
                    painter = { AppIcons.previous },
                    contentDescription = stringResource(Res.string.client_schedule_previous_week_action),
                    onClick = { onEvent(ClientScheduleEvent.OnPreviousWeekClicked) },
                )
            } else {
                TopBarAction.None
            },
            action = if (isWeekLoaded) {
                TopBarAction.Icon(
                    painter = { AppIcons.next },
                    contentDescription = stringResource(Res.string.client_schedule_next_week_action),
                    onClick = { onEvent(ClientScheduleEvent.OnNextWeekClicked) },
                )
            } else {
                TopBarAction.None
            },
        )
        if (state.coaches.size > 1) {
            CoachSelector(state = state, onEvent = onEvent)
        }
        AppWeekStrip(
            days = state.days.map { toWeekDay(day = it, isSelected = it.date == state.selectedDate) },
            onSelect = { dayId ->
                state.days.firstOrNull { it.date.toString() == dayId }?.let { day ->
                    onEvent(ClientScheduleEvent.OnDateSelected(day.date))
                }
            },
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ClientScheduleEvent.OnRetryClicked) },
                )
                state.isLoading -> AppSlotShimmerList(count = SHIMMER_SLOTS)
                state.coaches.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.client_schedule_no_coach_title),
                    description = stringResource(Res.string.client_schedule_no_coach_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.client_schedule_no_coach_action),
                        onClick = { onEvent(ClientScheduleEvent.OnRetryClicked) },
                    ),
                )
                state.days.all { it.slots.isEmpty() } -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.client_schedule_empty_title),
                    description = stringResource(Res.string.client_schedule_empty_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.client_schedule_empty_action),
                        onClick = { onEvent(ClientScheduleEvent.OnWriteCoachClicked) },
                    ),
                )
                selectedSlots(state).isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.client_schedule_day_empty_title),
                    description = stringResource(Res.string.client_schedule_day_empty_description),
                    action = PlaceholderAction.None,
                )
                else -> SlotList(slots = selectedSlots(state), onEvent = onEvent)
            }
        }
    }
    if (state.slotPendingCancel != null) {
        AppConfirmDialog(
            title = stringResource(Res.string.client_schedule_cancel_dialog_title),
            description = stringResource(Res.string.client_schedule_cancel_dialog_description),
            confirmText = stringResource(Res.string.client_schedule_cancel_dialog_confirm),
            onConfirm = { onEvent(ClientScheduleEvent.OnCancelConfirmed) },
            onDismissRequest = { onEvent(ClientScheduleEvent.OnCancelDismissed) },
            tone = ConfirmDialogTone.Danger,
        )
    }
}

@Composable
private fun CoachSelector(state: ClientScheduleState, onEvent: (ClientScheduleEvent) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.dp16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        items(items = state.coaches, key = { it.coachId }) { coach ->
            AppButton(
                text = coach.displayName,
                onClick = { onEvent(ClientScheduleEvent.OnCoachSelected(coach.coachId)) },
                tone = if (coach.coachId == state.selectedCoachId) {
                    ButtonTone.Primary
                } else {
                    ButtonTone.Secondary
                },
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun SlotList(slots: List<ClientSlotRow>, onEvent: (ClientScheduleEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        items(items = slots, key = { it.slotId }) { slot ->
            AppClientSlotCard(
                time = slot.timeLabel,
                duration = slot.durationLabel,
                seats = slot.seatsLabel,
                availability = when {
                    slot.isBookedByMe -> ClientSlotAvailability.Mine
                    slot.isAvailable -> ClientSlotAvailability.Free
                    else -> ClientSlotAvailability.TakenBySomeone
                },
                request = if (slot.hasPendingChangeRequest) {
                    SlotRequestView.Cancel(onApprove = { }, onReject = { })
                } else {
                    SlotRequestView.None
                },
                action = toAction(slot = slot, onEvent = onEvent),
                note = slot.note,
            )
        }
        item(key = "privacy-note") {
            AppText(
                modifier = Modifier.padding(top = AppTheme.spacing.dp8),
                text = stringResource(Res.string.client_schedule_privacy_note),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textMuted,
            )
        }
    }
}

private fun toAction(
    slot: ClientSlotRow,
    onEvent: (ClientScheduleEvent) -> Unit,
): ClientSlotAction = when {
    slot.hasPendingChangeRequest -> ClientSlotAction.None
    slot.isBookedByMe && slot.canRequestChange -> ClientSlotAction.Cancel(
        onClick = { onEvent(ClientScheduleEvent.OnCancelClicked(slot.slotId)) },
    )
    slot.isBookedByMe -> ClientSlotAction.None
    slot.isAvailable -> ClientSlotAction.Book(
        onClick = { onEvent(ClientScheduleEvent.OnBookClicked(slot.slotId)) },
    )
    slot.isOnWaitlist -> ClientSlotAction.LeaveWaitlist(
        onClick = { onEvent(ClientScheduleEvent.OnWaitlistToggled(slot.slotId)) },
    )
    else -> ClientSlotAction.JoinWaitlist(
        onClick = { onEvent(ClientScheduleEvent.OnWaitlistToggled(slot.slotId)) },
    )
}

private fun selectedSlots(state: ClientScheduleState): List<ClientSlotRow> =
    state.days.firstOrNull { it.date == state.selectedDate }?.slots.orEmpty()

private fun toWeekDay(day: ClientScheduleDay, isSelected: Boolean): WeekDay = WeekDay(
    id = day.date.toString(),
    weekdayLabel = day.weekdayLabel,
    dayNumber = day.dayNumberLabel,
    state = when {
        isSelected -> WeekDayState.Selected
        day.isToday -> WeekDayState.Today
        day.isWeekend -> WeekDayState.Weekend
        else -> WeekDayState.Rest
    },
    hasSlots = day.slots.isNotEmpty(),
)
