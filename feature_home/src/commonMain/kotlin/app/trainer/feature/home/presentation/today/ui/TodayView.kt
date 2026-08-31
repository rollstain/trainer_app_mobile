package app.trainer.feature.home.presentation.today.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.data.schedule.SlotChangeKind
import app.trainer.feature.home.presentation.today.mvi.LapsedSince
import app.trainer.feature.home.presentation.today.mvi.TodayBlock
import app.trainer.feature.home.presentation.today.mvi.TodayDialogRow
import app.trainer.feature.home.presentation.today.mvi.TodayEvent
import app.trainer.feature.home.presentation.today.mvi.TodayFreeSlots
import app.trainer.feature.home.presentation.today.mvi.TodayNextSession
import app.trainer.feature.home.presentation.today.mvi.TodayRequestRow
import app.trainer.feature.home.presentation.today.mvi.TodaySessionRow
import app.trainer.feature.home.presentation.today.mvi.TodayState
import app.trainer.feature.home.presentation.today.mvi.TodayTomorrow
import app.trainer.strings.Res
import app.trainer.strings.home_days_short
import app.trainer.strings.home_never_logged
import app.trainer.strings.home_profile_action
import app.trainer.strings.today_add_slot_action
import app.trainer.strings.today_block_failed
import app.trainer.strings.today_block_retry
import app.trainer.strings.today_check_ins_all
import app.trainer.strings.today_check_ins_title
import app.trainer.strings.today_form_checks_all
import app.trainer.strings.today_form_checks_title
import app.trainer.strings.today_lapsed_title
import app.trainer.strings.today_more_dialogs
import app.trainer.strings.today_next_session_title
import app.trainer.strings.today_no_clients_description
import app.trainer.strings.today_no_clients_title
import app.trainer.strings.today_no_sessions_this_week
import app.trainer.strings.today_open_calendar_action
import app.trainer.strings.today_quiet_no_check_ins
import app.trainer.strings.today_quiet_no_lapsed
import app.trainer.strings.today_quiet_no_requests
import app.trainer.strings.today_quiet_no_unread
import app.trainer.strings.today_requests_title
import app.trainer.strings.today_sessions_title
import app.trainer.strings.today_title
import app.trainer.strings.today_tomorrow_title
import app.trainer.strings.today_unread_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppBlockFallback
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCoachSlotCard
import app.trainer.uikit.widgets.AppIcon
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppListCell
import app.trainer.uikit.widgets.AppSectionHeader
import app.trainer.uikit.widgets.AppSlotRow
import app.trainer.uikit.widgets.AppSlotShimmerList
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.IconSize
import app.trainer.uikit.widgets.ListCellPreview
import app.trainer.uikit.widgets.ListCellSize
import app.trainer.uikit.widgets.ListCellTrailing
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.SeatsState
import app.trainer.uikit.widgets.SectionCount
import app.trainer.uikit.widgets.SlotClientView
import app.trainer.uikit.widgets.SlotRequestView
import app.trainer.uikit.widgets.SlotRowNote
import app.trainer.uikit.widgets.SlotRowStatus
import app.trainer.uikit.widgets.SlotRowTrailing
import app.trainer.uikit.widgets.SlotStatusView
import app.trainer.uikit.widgets.TopBarAction
import app.trainer.uikit.widgets.TopBarSubtitle
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_ROWS = 4

@Composable
fun TodayView(
    modifier: Modifier = Modifier,
    state: TodayState,
    onEvent: (TodayEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.today_title),
            subtitle = TopBarSubtitle.Text(state.dateLabel),
            action = TopBarAction.Avatar(
                displayName = state.coachDisplayName,
                contentDescription = stringResource(Res.string.home_profile_action),
                onClick = { onEvent(TodayEvent.OnProfileClicked) },
            ),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> AppSlotShimmerList(count = SHIMMER_ROWS)
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(TodayEvent.OnRetryClicked) },
                )
                !state.hasClients -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.today_no_clients_title),
                    description = stringResource(Res.string.today_no_clients_description),
                    action = PlaceholderAction.None,
                )
                else -> TodayContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun TodayContent(state: TodayState, onEvent: (TodayEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppTheme.spacing.dp24),
    ) {
        if (state.isQuiet) {
            quietBlocks(state = state, onEvent = onEvent)
        } else {
            requestsBlock(requests = state.requests, onEvent = onEvent)
            sessionsBlock(sessions = state.sessions, onEvent = onEvent)
            unreadBlock(state = state, onEvent = onEvent)
            checkInsBlock(state = state, onEvent = onEvent)
            formChecksBlock(state = state, onEvent = onEvent)
            lapsedBlock(state = state, onEvent = onEvent)
            tomorrowBlock(tomorrow = state.tomorrow, onEvent = onEvent)
        }
    }
}

private fun LazyListScope.requestsBlock(
    requests: List<TodayRequestRow>,
    onEvent: (TodayEvent) -> Unit,
) {
    if (requests.isEmpty()) return
    item(key = "requests-title") {
        AppSectionHeader(
            title = stringResource(Res.string.today_requests_title),
            count = SectionCount.Value(requests.size),
        )
    }
    items(items = requests, key = { "request-${it.requestId}" }) { request ->
        Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp4)) {
            AppCoachSlotCard(
                time = request.timeLabel,
                duration = "",
                status = SlotStatusView.Booked,
                client = request.requestedByDisplayName
                    ?.let(SlotClientView::Booked)
                    ?: SlotClientView.Nobody,
                request = requestViewOf(request = request, onEvent = onEvent),
            )
        }
    }
}

private fun requestViewOf(request: TodayRequestRow, onEvent: (TodayEvent) -> Unit): SlotRequestView {
    val onApprove = { onEvent(TodayEvent.OnRequestResolved(request.requestId, approve = true)) }
    val onReject = { onEvent(TodayEvent.OnRequestResolved(request.requestId, approve = false)) }
    return when (request.kind) {
        SlotChangeKind.CANCEL -> SlotRequestView.Cancel(onApprove = onApprove, onReject = onReject)
        SlotChangeKind.RESCHEDULE -> SlotRequestView.Reschedule(
            proposedTime = request.proposedTimeLabel.orEmpty(),
            onApprove = onApprove,
            onReject = onReject,
        )
    }
}

private fun LazyListScope.sessionsBlock(
    sessions: List<TodaySessionRow>,
    onEvent: (TodayEvent) -> Unit,
) {
    if (sessions.isEmpty()) return
    item(key = "sessions-title") {
        AppSectionHeader(
            title = stringResource(Res.string.today_sessions_title),
            count = SectionCount.Value(sessions.size),
        )
    }
    items(items = sessions, key = { "session-${it.slotId}" }) { session ->
        AppSlotRow(
            timeLabel = session.timeLabel,
            durationLabel = session.durationLabel,
            title = session.clientDisplayName,
            status = SlotRowStatus.Booked,
            trailing = if (session.seatsLabel.isEmpty()) {
                SlotRowTrailing.Client(displayName = session.clientDisplayName)
            } else {
                SlotRowTrailing.Seats(
                    label = session.seatsLabel,
                    state = SeatsState.of(taken = session.takenSeats, capacity = session.capacity),
                )
            },
            participants = session.participants,
            onClick = { onEvent(TodayEvent.OnSessionClicked(session.clientUserId)) },
            note = if (session.isNext) SlotRowNote.Text(session.startsInLabel) else SlotRowNote.None,
            isNext = session.isNext,
        )
    }
}

private fun LazyListScope.unreadBlock(state: TodayState, onEvent: (TodayEvent) -> Unit) {
    if (state.unread.isEmpty()) return
    item(key = "unread-title") {
        AppSectionHeader(
            title = stringResource(Res.string.today_unread_title),
            count = SectionCount.Value(state.unread.size + state.moreUnreadCount),
        )
    }
    items(items = state.unread, key = { "dialog-${it.dialogId}" }) { dialog ->
        DialogCell(dialog = dialog, onEvent = onEvent)
    }
    if (state.moreUnreadCount > 0) {
        item(key = "more-dialogs") {
            AppButton(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16),
                text = stringResource(Res.string.today_more_dialogs, state.moreUnreadCount),
                onClick = { onEvent(TodayEvent.OnAllDialogsClicked) },
                tone = ButtonTone.Text,
                size = ButtonSize.Large,
            )
        }
    }
}

@Composable
private fun DialogCell(dialog: TodayDialogRow, onEvent: (TodayEvent) -> Unit) {
    AppListCell(
        title = dialog.peerDisplayName,
        onClick = { onEvent(TodayEvent.OnDialogClicked(dialog.dialogId)) },
        size = ListCellSize.Small,
        preview = ListCellPreview.Text(dialog.preview),
        trailing = ListCellTrailing.Unread(unreadCount = dialog.unreadCount),
    )
}

private fun LazyListScope.checkInsBlock(
    state: TodayState,
    onEvent: (TodayEvent) -> Unit,
) {
    val rows = state.awaitingCheckIns
    val hasFailed = state.failedBlocks.contains(TodayBlock.CheckIns)
    if (rows.isEmpty() && !hasFailed) return
    item(key = "check-ins-title") {
        AppSectionHeader(
            title = stringResource(Res.string.today_check_ins_title),
            count = if (hasFailed || state.hasMoreCheckIns) SectionCount.None else SectionCount.Value(rows.size),
        )
    }
    if (hasFailed) {
        blockFallback(key = "check-ins-fallback", block = TodayBlock.CheckIns, onEvent = onEvent)
        return
    }
    items(items = rows, key = { "check-in-${it.checkInId}" }) { row ->
        AppListCell(
            title = row.displayName,
            onClick = { onEvent(TodayEvent.OnCheckInClicked(row.clientUserId)) },
            size = ListCellSize.Small,
            trailing = ListCellTrailing.Time(value = row.dateLabel),
        )
    }
    if (state.hasMoreCheckIns) {
        item(key = "check-ins-all") {
            AppListCell(
                title = stringResource(Res.string.today_check_ins_all),
                onClick = { onEvent(TodayEvent.OnAllCheckInsClicked) },
                size = ListCellSize.Small,
            )
        }
    }
}

private fun LazyListScope.formChecksBlock(
    state: TodayState,
    onEvent: (TodayEvent) -> Unit,
) {
    val rows = state.awaitingFormChecks
    val hasFailed = state.failedBlocks.contains(TodayBlock.FormChecks)
    if (rows.isEmpty() && !hasFailed) return
    item(key = "form-checks-title") {
        AppSectionHeader(
            title = stringResource(Res.string.today_form_checks_title),
            count = if (hasFailed || state.hasMoreFormChecks) {
                SectionCount.None
            } else {
                SectionCount.Value(rows.size)
            },
        )
    }
    if (hasFailed) {
        blockFallback(key = "form-checks-fallback", block = TodayBlock.FormChecks, onEvent = onEvent)
        return
    }
    items(items = rows, key = { "form-check-${it.formCheckId}" }) { row ->
        AppListCell(
            title = row.displayName,
            onClick = { onEvent(TodayEvent.OnFormChecksClicked) },
            size = ListCellSize.Small,
            trailing = ListCellTrailing.Time(value = row.dateLabel),
        )
    }
    if (state.hasMoreFormChecks) {
        item(key = "form-checks-all") {
            AppListCell(
                title = stringResource(Res.string.today_form_checks_all),
                onClick = { onEvent(TodayEvent.OnFormChecksClicked) },
                size = ListCellSize.Small,
            )
        }
    }
}

private fun LazyListScope.lapsedBlock(state: TodayState, onEvent: (TodayEvent) -> Unit) {
    val lapsed = state.lapsed
    val hasFailed = state.failedBlocks.contains(TodayBlock.Lapsed)
    if (lapsed.isEmpty() && !hasFailed) return
    item(key = "lapsed-title") {
        AppSectionHeader(
            title = stringResource(Res.string.today_lapsed_title),
            count = if (hasFailed) SectionCount.None else SectionCount.Value(lapsed.size),
        )
    }
    if (hasFailed) {
        blockFallback(key = "lapsed-fallback", block = TodayBlock.Lapsed, onEvent = onEvent)
        return
    }
    items(items = lapsed, key = { "lapsed-${it.userId}" }) { row ->
        AppListCell(
            title = row.displayName,
            onClick = { onEvent(TodayEvent.OnLapsedClicked(row.userId)) },
            size = ListCellSize.Small,
            trailing = when (val since = row.since) {
                LapsedSince.Never -> ListCellTrailing.Warning(
                    text = stringResource(Res.string.home_never_logged)
                )
                is LapsedSince.Days -> ListCellTrailing.Alert(
                    value = since.value.toString(),
                    unit = stringResource(Res.string.home_days_short),
                )
            },
        )
    }
}

private fun LazyListScope.tomorrowBlock(tomorrow: TodayTomorrow, onEvent: (TodayEvent) -> Unit) {
    when (tomorrow) {
        TodayTomorrow.None -> Unit
        is TodayTomorrow.Sessions -> item(key = "tomorrow") {
            AppSectionHeader(title = stringResource(Res.string.today_tomorrow_title))
            Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
                AppCard(
                    action = app.trainer.uikit.widgets.CardAction.Click(
                        onClick = { onEvent(TodayEvent.OnCalendarClicked) }
                    ),
                ) {
                    AppText(
                        text = tomorrow.summary,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.quietBlocks(state: TodayState, onEvent: (TodayEvent) -> Unit) {
    item(key = "quiet-next") {
        Box(modifier = Modifier.padding(AppTheme.spacing.dp16)) {
            NextSessionCard(nextSession = state.nextSession, onEvent = onEvent)
        }
    }
    item(key = "quiet-checklist") {
        Column(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        ) {
            QuietLine(text = stringResource(Res.string.today_quiet_no_requests))
            QuietLine(text = stringResource(Res.string.today_quiet_no_unread))
            QuietLine(text = stringResource(Res.string.today_quiet_no_lapsed))
            QuietLine(text = stringResource(Res.string.today_quiet_no_check_ins))
        }
    }
    when (val freeSlots = state.freeSlots) {
        TodayFreeSlots.None -> Unit
        is TodayFreeSlots.Available -> item(key = "quiet-free-slots") {
            Column(
                modifier = Modifier.padding(AppTheme.spacing.dp16),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                AppText(
                    text = freeSlots.summary,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
                AppButton(
                    text = stringResource(Res.string.today_open_calendar_action),
                    onClick = { onEvent(TodayEvent.OnCalendarClicked) },
                    tone = ButtonTone.Secondary,
                    size = ButtonSize.Large,
                )
            }
        }
    }
}

@Composable
private fun NextSessionCard(nextSession: TodayNextSession, onEvent: (TodayEvent) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            when (nextSession) {
                TodayNextSession.NoneThisWeek -> {
                    AppText(
                        text = stringResource(Res.string.today_no_sessions_this_week),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppButton(
                        text = stringResource(Res.string.today_add_slot_action),
                        onClick = { onEvent(TodayEvent.OnAddSlotClicked) },
                        tone = ButtonTone.Primary,
                        size = ButtonSize.Large,
                    )
                }
                is TodayNextSession.Upcoming -> {
                    AppText(
                        text = stringResource(Res.string.today_next_session_title),
                        style = AppTheme.typography.overline,
                        color = AppTheme.colors.textMuted,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppText(
                            text = nextSession.timeLabel,
                            style = AppTheme.typography.display,
                            color = AppTheme.colors.textPrimary,
                        )
                        AppText(
                            modifier = Modifier.weight(1f),
                            text = nextSession.dayLabel,
                            style = AppTheme.typography.numeric,
                            color = AppTheme.colors.textSecondary,
                        )
                        AppText(
                            text = nextSession.startsInLabel,
                            style = AppTheme.typography.numeric,
                            color = AppTheme.colors.accent,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppAvatar(displayName = nextSession.clientDisplayName, size = AvatarSize.Small)
                        AppText(
                            text = nextSession.clientDisplayName,
                            style = AppTheme.typography.body,
                            color = AppTheme.colors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuietLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            painter = AppIcons.sent,
            contentDescription = null,
            size = IconSize.Large,
            tint = AppTheme.colors.success,
        )
        AppText(
            text = text,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
    }
}

private fun LazyListScope.blockFallback(
    key: String,
    block: TodayBlock,
    onEvent: (TodayEvent) -> Unit,
) {
    item(key = key) {
        Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
            AppBlockFallback(
                message = stringResource(Res.string.today_block_failed),
                retryText = stringResource(Res.string.today_block_retry),
                onRetry = { onEvent(TodayEvent.OnBlockRetryClicked(block)) },
            )
        }
    }
}
