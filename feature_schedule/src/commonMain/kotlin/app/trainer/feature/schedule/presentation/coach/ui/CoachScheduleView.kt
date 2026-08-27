package app.trainer.feature.schedule.presentation.coach.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.trainer.base.failure.AppFailureState
import app.trainer.data.schedule.SlotChangeKind
import app.trainer.data.schedule.SlotStatus
import app.trainer.feature.schedule.presentation.coach.mvi.ChangeRequestRow
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleEvent
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleState
import app.trainer.feature.schedule.presentation.coach.mvi.CoachSlotRow
import app.trainer.feature.schedule.presentation.coach.mvi.ScheduleDay
import app.trainer.feature.schedule.presentation.coach.mvi.SlotActions
import app.trainer.feature.schedule.presentation.coach.mvi.SlotActionsKind
import app.trainer.strings.Res
import app.trainer.strings.coach_schedule_add_slot_action
import app.trainer.strings.coach_schedule_cancel_session_action
import app.trainer.strings.coach_schedule_cancel_slot_action
import app.trainer.strings.coach_schedule_complete_action
import app.trainer.strings.coach_schedule_day_empty
import app.trainer.strings.coach_schedule_day_summary_both
import app.trainer.strings.coach_schedule_day_summary_busy
import app.trainer.strings.coach_schedule_day_summary_free
import app.trainer.strings.coach_schedule_empty_description
import app.trainer.strings.coach_schedule_next_week_action
import app.trainer.strings.coach_schedule_requests_title
import app.trainer.strings.coach_schedule_slot_actions_dismiss
import app.trainer.strings.coach_schedule_today_mark
import app.trainer.strings.slot_cancelled_chip
import app.trainer.strings.slot_completed_chip
import app.trainer.strings.slot_free_chip
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppBottomSheetContainer
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCoachSlotCard
import app.trainer.uikit.widgets.AppDaySectionHeader
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppSlotRow
import app.trainer.uikit.widgets.AppSlotShimmerList
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AppWeekStrip
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.DaySectionSummary
import app.trainer.uikit.widgets.SlotClientView
import app.trainer.uikit.widgets.SlotRequestView
import app.trainer.uikit.widgets.SlotRowStatus
import app.trainer.uikit.widgets.SlotRowTrailing
import app.trainer.uikit.widgets.SlotStatusView
import app.trainer.uikit.widgets.StatusChipKind
import app.trainer.uikit.widgets.TopBarAction
import app.trainer.uikit.widgets.WeekDay
import app.trainer.uikit.widgets.WeekDayState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_SLOTS = 4
private const val REQUESTS_ITEM_KEY = "requests"
private const val WEEK_FOOTER_ITEM_KEY = "week-footer"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoachScheduleView(
    modifier: Modifier = Modifier,
    state: CoachScheduleState,
    onEvent: (CoachScheduleEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val visibleDate = visibleDateOf(listState = listState, state = state)

    ScrollToTodayOnOpen(listState = listState, state = state)

    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = state.weekTitle,
            action = TopBarAction.Icon(
                painter = { AppIcons.next },
                contentDescription = stringResource(Res.string.coach_schedule_next_week_action),
                onClick = { onEvent(CoachScheduleEvent.OnNextWeekClicked) },
            ),
        )
        AppWeekStrip(
            days = state.days.map { toWeekDay(day = it, isSelected = it.date == visibleDate) },
            onSelect = { dayId ->
                state.days.firstOrNull { it.date.toString() == dayId }?.let { day ->
                    scope.launch {
                        listState.animateScrollToItem(dayHeaderIndexOf(state = state, date = day.date))
                    }
                }
            },
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(CoachScheduleEvent.OnRetryClicked) },
                )
                state.isLoading -> AppSlotShimmerList(count = SHIMMER_SLOTS)
                else -> WeekList(
                    listState = listState,
                    state = state,
                    visibleDate = visibleDate,
                    onEvent = onEvent,
                )
            }
        }
        state.slotActions?.let { actions ->
            SlotActionsSheet(actions = actions, onEvent = onEvent)
        }
    }
}

@Composable
private fun SlotActionsSheet(actions: SlotActions, onEvent: (CoachScheduleEvent) -> Unit) {
    val buttonState = if (actions.isResolving) ButtonState.Loading else ButtonState.Idle
    AppBottomSheetContainer(title = actions.title) {
        when (actions.kind) {
            SlotActionsKind.Booked -> AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.coach_schedule_complete_action),
                onClick = { onEvent(CoachScheduleEvent.OnCompleteSlotClicked(actions.slotId)) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Large,
                state = buttonState,
            )
            SlotActionsKind.Free -> Unit
        }
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = when (actions.kind) {
                SlotActionsKind.Booked -> stringResource(Res.string.coach_schedule_cancel_session_action)
                SlotActionsKind.Free -> stringResource(Res.string.coach_schedule_cancel_slot_action)
            },
            onClick = { onEvent(CoachScheduleEvent.OnCancelSlotClicked(actions.slotId)) },
            tone = ButtonTone.Danger,
            size = ButtonSize.Large,
            state = buttonState,
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.coach_schedule_slot_actions_dismiss),
            onClick = { onEvent(CoachScheduleEvent.OnSlotActionsDismissed) },
            tone = ButtonTone.Text,
            size = ButtonSize.Large,
        )
    }
}

@Composable
private fun WeekList(
    listState: LazyListState,
    state: CoachScheduleState,
    visibleDate: LocalDate?,
    onEvent: (CoachScheduleEvent) -> Unit,
) {
    val todayMark = stringResource(Res.string.coach_schedule_today_mark)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
    ) {
        if (state.pendingRequests.isNotEmpty()) {
            item(key = REQUESTS_ITEM_KEY) {
                RequestsBlock(requests = state.pendingRequests, onEvent = onEvent)
            }
        }
        state.days.forEach { day ->
            stickyHeader(key = "day-${day.date}") {
                AppDaySectionHeader(
                    dayLabel = "${day.weekdayLabel} ${day.dayNumberLabel}",
                    isToday = day.isToday,
                    todayLabel = todayMark,
                    summary = DaySectionSummary.Text(daySummaryOf(day)),
                )
            }
            items(items = day.slots, key = { it.slotId }) { slot ->
                AppSlotRow(
                    timeLabel = slot.timeLabel,
                    durationLabel = slot.durationLabel,
                    title = slotTitleOf(slot),
                    status = toRowStatus(slot.status),
                    trailing = slotTrailingOf(slot),
                    onClick = { onEvent(CoachScheduleEvent.OnSlotClicked(slot.slotId)) },
                    hasRequest = slot.hasPendingChangeRequest,
                    isNext = slot.slotId == state.nextSlotId,
                )
            }
        }
        item(key = WEEK_FOOTER_ITEM_KEY) {
            WeekFooter(
                isWeekEmpty = state.days.all { it.slots.isEmpty() },
                onAddSlot = { onEvent(CoachScheduleEvent.OnCreateSlotClicked(visibleDate)) },
            )
        }
    }
}

@Composable
private fun WeekFooter(isWeekEmpty: Boolean, onAddSlot: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        if (isWeekEmpty) {
            AppText(
                text = stringResource(Res.string.coach_schedule_empty_description),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.coach_schedule_add_slot_action),
            onClick = onAddSlot,
            tone = if (isWeekEmpty) ButtonTone.Primary else ButtonTone.Secondary,
            size = ButtonSize.Large,
        )
    }
}

@Composable
private fun daySummaryOf(day: ScheduleDay): String {
    if (day.slots.isEmpty()) return stringResource(Res.string.coach_schedule_day_empty)
    val free = day.slots.count { it.status == SlotStatus.FREE }
    val busy = day.slots.count { it.status == SlotStatus.BOOKED || it.status == SlotStatus.COMPLETED }
    return when {
        busy == 0 && free == 0 -> stringResource(Res.string.coach_schedule_day_empty)
        busy > 0 && free > 0 -> stringResource(Res.string.coach_schedule_day_summary_both, busy, free)
        busy > 0 -> stringResource(Res.string.coach_schedule_day_summary_busy, busy)
        else -> stringResource(Res.string.coach_schedule_day_summary_free, free)
    }
}

private fun slotTitleOf(slot: CoachSlotRow): String = slot.clientDisplayName.orEmpty()

@Composable
private fun slotTrailingOf(slot: CoachSlotRow): SlotRowTrailing = when (slot.status) {
    SlotStatus.BOOKED ->
        slot.clientDisplayName
            ?.let(SlotRowTrailing::Client)
            ?: SlotRowTrailing.Status(
                text = stringResource(Res.string.slot_free_chip),
                kind = StatusChipKind.Free,
            )
    SlotStatus.FREE -> SlotRowTrailing.Status(
        text = stringResource(Res.string.slot_free_chip),
        kind = StatusChipKind.Free,
    )
    SlotStatus.CANCELLED -> SlotRowTrailing.Status(
        text = stringResource(Res.string.slot_cancelled_chip),
        kind = StatusChipKind.Cancelled,
    )
    SlotStatus.COMPLETED -> SlotRowTrailing.Status(
        text = stringResource(Res.string.slot_completed_chip),
        kind = StatusChipKind.Completed,
    )
}

private fun toRowStatus(status: SlotStatus): SlotRowStatus = when (status) {
    SlotStatus.FREE -> SlotRowStatus.Free
    SlotStatus.BOOKED -> SlotRowStatus.Booked
    SlotStatus.CANCELLED -> SlotRowStatus.Cancelled
    SlotStatus.COMPLETED -> SlotRowStatus.Completed
}

private fun dayHeaderIndexOf(state: CoachScheduleState, date: LocalDate): Int {
    var index = if (state.pendingRequests.isEmpty()) 0 else 1
    state.days.forEach { day ->
        if (day.date == date) return index
        index += 1 + day.slots.size
    }
    return index
}

@Composable
private fun RequestsBlock(
    requests: ImmutableList<ChangeRequestRow>,
    onEvent: (CoachScheduleEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = stringResource(Res.string.coach_schedule_requests_title),
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = requests.size.toString(),
                style = AppTheme.typography.overline,
                color = AppTheme.colors.warning,
            )
        }
        requests.forEach { request ->
            AppCoachSlotCard(
                time = request.slotTimeLabel,
                duration = "",
                status = SlotStatusView.Booked,
                client = request.requestedByDisplayName
                    ?.let(SlotClientView::Booked)
                    ?: SlotClientView.Nobody,
                request = toRequestView(request = request, onEvent = onEvent),
            )
        }
    }
}

private fun toRequestView(
    request: ChangeRequestRow,
    onEvent: (CoachScheduleEvent) -> Unit,
): SlotRequestView {
    val onApprove = {
        onEvent(
            CoachScheduleEvent.OnChangeRequestResolved(requestId = request.requestId, approve = true)
        )
    }
    val onReject = {
        onEvent(
            CoachScheduleEvent.OnChangeRequestResolved(requestId = request.requestId, approve = false)
        )
    }
    return when (request.kind) {
        SlotChangeKind.RESCHEDULE -> SlotRequestView.Reschedule(
            proposedTime = request.proposedTimeLabel.orEmpty(),
            onApprove = onApprove,
            onReject = onReject,
        )
        SlotChangeKind.CANCEL -> SlotRequestView.Cancel(onApprove = onApprove, onReject = onReject)
    }
}

private fun toWeekDay(day: ScheduleDay, isSelected: Boolean): WeekDay = WeekDay(
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

@Composable
private fun ScrollToTodayOnOpen(listState: LazyListState, state: CoachScheduleState) {
    val todayDate = state.days.firstOrNull { it.isToday }?.date
    LaunchedEffect(state.weekStart, todayDate, state.days.size) {
        if (todayDate == null) return@LaunchedEffect
        listState.scrollToItem(dayHeaderIndexOf(state = state, date = todayDate))
    }
}

@Composable
private fun visibleDateOf(listState: LazyListState, state: CoachScheduleState): LocalDate? {
    val firstVisible by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    return remember(firstVisible, state.days, state.pendingRequests.size) {
        state.days.lastOrNull { dayHeaderIndexOf(state = state, date = it.date) <= firstVisible }
            ?.date
            ?: state.days.firstOrNull()?.date
    }
}
