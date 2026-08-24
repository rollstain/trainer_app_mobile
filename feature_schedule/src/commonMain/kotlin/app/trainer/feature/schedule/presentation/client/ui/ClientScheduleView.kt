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
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleDay
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleEvent
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleState
import app.trainer.feature.schedule.presentation.client.mvi.ClientSlotRow
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppClientSlotCard
import app.trainer.uikit.widgets.AppConfirmDialog
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
import app.trainer.uikit.widgets.WeekDay
import app.trainer.uikit.widgets.WeekDayState

private const val TITLE = "Запись"
private const val PRIVACY_NOTE = "Кто занял соседнее время, не показывается."
private const val EMPTY_TITLE = "У тренера пока нет свободного времени"
private const val EMPTY_DESCRIPTION =
    "Как только он откроет запись, время появится здесь. Если нужно раньше — напишите ему."
private const val EMPTY_ACTION = "Написать тренеру"
private const val NO_COACH_TITLE = "Вы ещё не связаны с тренером"
private const val NO_COACH_DESCRIPTION = "Введите код приглашения — он есть в сообщении от тренера."
private const val NO_COACH_ACTION = "Ввести код"
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"
private const val CANCEL_DIALOG_TITLE = "Отменить тренировку?"
private const val CANCEL_DIALOG_DESCRIPTION =
    "Тренер получит заявку и подтвердит отмену. Время станет свободным для других."
private const val CANCEL_DIALOG_CONFIRM = "Отменить запись"

@Composable
fun ClientScheduleView(
    modifier: Modifier = Modifier,
    state: ClientScheduleState,
    onEvent: (ClientScheduleEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = if (state.weekTitle.isEmpty()) TITLE else state.weekTitle)
        if (state.coaches.size > 1) {
            CoachSelector(state = state, onEvent = onEvent)
        }
        AppWeekStrip(
            days = state.days.map(::toWeekDay),
            onSelect = { dayId ->
                state.days.firstOrNull { it.date.toString() == dayId }?.let { day ->
                    onEvent(ClientScheduleEvent.OnDateSelected(day.date))
                }
            },
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isFailed -> AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(ClientScheduleEvent.OnRetryClicked) },
                    ),
                )
                state.coaches.isEmpty() && !state.isLoading -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = NO_COACH_TITLE,
                    description = NO_COACH_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = NO_COACH_ACTION,
                        onClick = { onEvent(ClientScheduleEvent.OnRetryClicked) },
                    ),
                )
                selectedSlots(state).isEmpty() && !state.isLoading -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EMPTY_TITLE,
                    description = EMPTY_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = EMPTY_ACTION,
                        onClick = { onEvent(ClientScheduleEvent.OnRetryClicked) },
                    ),
                )
                else -> SlotList(slots = selectedSlots(state), onEvent = onEvent)
            }
        }
    }
    if (state.slotPendingCancel != null) {
        AppConfirmDialog(
            title = CANCEL_DIALOG_TITLE,
            description = CANCEL_DIALOG_DESCRIPTION,
            confirmText = CANCEL_DIALOG_CONFIRM,
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
                text = PRIVACY_NOTE,
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
    state.days.firstOrNull { it.isSelected }?.slots.orEmpty()

private fun toWeekDay(day: ClientScheduleDay): WeekDay = WeekDay(
    id = day.date.toString(),
    weekdayLabel = day.weekdayLabel,
    dayNumber = day.dayNumberLabel,
    state = when {
        day.isSelected -> WeekDayState.Selected
        day.isToday -> WeekDayState.Today
        day.isWeekend -> WeekDayState.Weekend
        else -> WeekDayState.Rest
    },
    hasSlots = day.slots.isNotEmpty(),
)
