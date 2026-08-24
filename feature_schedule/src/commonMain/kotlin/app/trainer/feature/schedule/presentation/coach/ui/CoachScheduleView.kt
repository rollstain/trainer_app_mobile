package app.trainer.feature.schedule.presentation.coach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.data.schedule.SlotChangeKind
import app.trainer.data.schedule.SlotStatus
import app.trainer.feature.schedule.presentation.coach.mvi.ChangeRequestRow
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleEvent
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleState
import app.trainer.feature.schedule.presentation.coach.mvi.CoachSlotRow
import app.trainer.feature.schedule.presentation.coach.mvi.ScheduleDay
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppCoachSlotCard
import app.trainer.uikit.widgets.AppDayTimeline
import app.trainer.uikit.widgets.AppIconButton
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AppWeekStrip
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.SlotClientView
import app.trainer.uikit.widgets.SlotRequestView
import app.trainer.uikit.widgets.SlotStatusView
import app.trainer.uikit.widgets.TimelineSlot
import app.trainer.uikit.widgets.TopBarAction
import app.trainer.uikit.widgets.WeekDay
import app.trainer.uikit.widgets.WeekDayState
import kotlinx.collections.immutable.ImmutableList

private const val REQUESTS_TITLE = "Заявки"
private const val EMPTY_TITLE = "На этой неделе пусто"
private const val EMPTY_DESCRIPTION = "Поставьте слоты — подопечные увидят их сразу."
private const val EMPTY_ACTION = "Добавить слот"
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"

@Composable
fun CoachScheduleView(
    modifier: Modifier = Modifier,
    state: CoachScheduleState,
    onEvent: (CoachScheduleEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = state.weekTitle,
            action = TopBarAction.Content(
                onClick = { onEvent(CoachScheduleEvent.OnNextWeekClicked) },
                render = {
                    AppIconButton(
                        painter = AppIcons.next,
                        contentDescription = "Следующая неделя",
                        onClick = { onEvent(CoachScheduleEvent.OnNextWeekClicked) },
                    )
                },
            ),
        )
        AppWeekStrip(
            days = state.days.map(::toWeekDay),
            onSelect = { dayId ->
                state.days.firstOrNull { it.date.toString() == dayId }?.let { day ->
                    onEvent(CoachScheduleEvent.OnDateSelected(day.date))
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
                        onClick = { onEvent(CoachScheduleEvent.OnRetryClicked) },
                    ),
                )
                state.days.all { it.slots.isEmpty() } && !state.isLoading -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EMPTY_TITLE,
                    description = EMPTY_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = EMPTY_ACTION,
                        onClick = { onEvent(CoachScheduleEvent.OnCreateSlotClicked) },
                    ),
                )
                else -> DayContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun DayContent(state: CoachScheduleState, onEvent: (CoachScheduleEvent) -> Unit) {
    val selectedDay = state.days.firstOrNull { it.isSelected }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        if (state.pendingRequests.isNotEmpty()) {
            RequestsBlock(requests = state.pendingRequests, onEvent = onEvent)
        }
        AppDayTimeline(
            slots = selectedDay?.slots.orEmpty().map(::toTimelineSlot),
            onSlotClick = { slotId -> onEvent(CoachScheduleEvent.OnSlotClicked(slotId)) },
        )
    }
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
                text = REQUESTS_TITLE,
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

private fun toWeekDay(day: ScheduleDay): WeekDay = WeekDay(
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

private fun toTimelineSlot(slot: CoachSlotRow): TimelineSlot = TimelineSlot(
    id = slot.slotId,
    startMinutes = slot.startMinutesOfDay,
    durationMinutes = slot.durationMinutes,
    timeLabel = slot.timeLabel,
    title = slot.clientDisplayName ?: "Свободно",
    durationLabel = slot.durationLabel,
    status = when (slot.status) {
        SlotStatus.FREE -> SlotStatusView.Free
        SlotStatus.BOOKED -> SlotStatusView.Booked
        SlotStatus.CANCELLED -> SlotStatusView.Cancelled
        SlotStatus.COMPLETED -> SlotStatusView.Completed
    },
    hasRequest = slot.hasPendingChangeRequest,
)
