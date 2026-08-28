package app.trainer.feature.schedule.presentation.groupsession.ui

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
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupParticipantRow
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupSessionEvent
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupSessionState
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupWaitingRow
import app.trainer.strings.Res
import app.trainer.strings.group_session_add_action
import app.trainer.strings.group_session_booked
import app.trainer.strings.group_session_cancel_action
import app.trainer.strings.group_session_complete_action
import app.trainer.strings.group_session_empty_description
import app.trainer.strings.group_session_empty_title
import app.trainer.strings.group_session_free
import app.trainer.strings.group_session_participants
import app.trainer.strings.group_session_pick_dismiss
import app.trainer.strings.group_session_pick_title
import app.trainer.strings.group_session_remove_action
import app.trainer.strings.group_session_waiting
import app.trainer.strings.group_session_waiting_book
import app.trainer.strings.group_session_waitlist_title
import app.trainer.strings.people_medical_badge
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppBottomSheetContainer
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCellShimmerList
import app.trainer.uikit.widgets.AppListCell
import app.trainer.uikit.widgets.AppSectionHeader
import app.trainer.uikit.widgets.AppStatTile
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardAction
import app.trainer.uikit.widgets.SectionCount
import app.trainer.uikit.widgets.StatTileTone
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.TopBarSubtitle
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_ROWS = 4

@Composable
fun GroupSessionView(
    modifier: Modifier = Modifier,
    state: GroupSessionState,
    onEvent: (GroupSessionEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = state.title,
            subtitle = TopBarSubtitle.Text(state.whenLabel),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(GroupSessionEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCellShimmerList(count = SHIMMER_ROWS)
                else -> SessionContent(state = state, onEvent = onEvent)
            }
        }
        if (state.isOpenForChanges) {
            SessionActions(state = state, onEvent = onEvent)
        }
        state.picker?.let { picker ->
            AppBottomSheetContainer(title = stringResource(Res.string.group_session_pick_title)) {
                picker.forEach { client ->
                    AppListCell(
                        title = client.displayName,
                        onClick = { onEvent(GroupSessionEvent.OnClientPicked(client.clientUserId)) },
                    )
                }
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.group_session_pick_dismiss),
                    onClick = { onEvent(GroupSessionEvent.OnPickerDismissed) },
                    tone = ButtonTone.Text,
                    size = ButtonSize.Medium,
                )
            }
        }
    }
}

@Composable
private fun SessionContent(state: GroupSessionState, onEvent: (GroupSessionEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        item(key = "tiles") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                AppStatTile(
                    modifier = Modifier.weight(1f),
                    value = state.takenSeats.toString(),
                    caption = stringResource(Res.string.group_session_booked),
                )
                AppStatTile(
                    modifier = Modifier.weight(1f),
                    value = state.freeSeats.toString(),
                    caption = stringResource(Res.string.group_session_free),
                    tone = if (state.hasFreeSeats) StatTileTone.Success else StatTileTone.Neutral,
                )
                AppStatTile(
                    modifier = Modifier.weight(1f),
                    value = state.waiting.size.toString(),
                    caption = stringResource(Res.string.group_session_waiting),
                    tone = if (state.waiting.isEmpty()) StatTileTone.Neutral else StatTileTone.Warning,
                )
            }
        }
        if (state.isEmptySession) {
            item(key = "empty") {
                Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
                    AppCard {
                        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                            AppText(
                                text = stringResource(Res.string.group_session_empty_title),
                                style = AppTheme.typography.bodyStrong,
                                color = AppTheme.colors.textPrimary,
                            )
                            AppText(
                                text = stringResource(Res.string.group_session_empty_description),
                                style = AppTheme.typography.body,
                                color = AppTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }
        } else {
            item(key = "participants-title") {
                AppSectionHeader(
                    title = stringResource(Res.string.group_session_participants),
                    count = SectionCount.Value(state.participants.size),
                )
            }
            items(items = state.participants, key = { "participant-${it.clientUserId}" }) { participant ->
                ParticipantRow(participant = participant, onEvent = onEvent)
            }
        }
        if (state.waiting.isNotEmpty()) {
            item(key = "waiting-title") {
                AppSectionHeader(
                    title = stringResource(Res.string.group_session_waitlist_title),
                    count = SectionCount.Value(state.waiting.size),
                )
            }
            items(items = state.waiting, key = { "waiting-${it.clientUserId}" }) { waiting ->
                WaitingRow(waiting = waiting, canBook = state.hasFreeSeats, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: GroupParticipantRow, onEvent: (GroupSessionEvent) -> Unit) {
    AppCard(
        modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16),
        action = CardAction.Click {
            onEvent(GroupSessionEvent.OnParticipantOpened(participant.clientUserId))
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppAvatar(displayName = participant.displayName, size = AvatarSize.Small)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        text = participant.displayName,
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textPrimary,
                    )
                    if (participant.hasMedicalNotes) {
                        MedicalBadge()
                    }
                }
                AppText(
                    text = participant.bookedAtLabel,
                    style = AppTheme.typography.numeric,
                    color = AppTheme.colors.textSecondary,
                )
            }
            AppButton(
                text = stringResource(Res.string.group_session_remove_action),
                onClick = { onEvent(GroupSessionEvent.OnParticipantRemoved(participant.clientUserId)) },
                tone = ButtonTone.Text,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun WaitingRow(
    waiting: GroupWaitingRow,
    canBook: Boolean,
    onEvent: (GroupSessionEvent) -> Unit,
) {
    AppCard(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppAvatar(displayName = waiting.displayName, size = AvatarSize.Small)
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = waiting.displayName,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = waiting.joinedAtLabel,
                    style = AppTheme.typography.numeric,
                    color = AppTheme.colors.textSecondary,
                )
            }
            AppButton(
                text = stringResource(Res.string.group_session_waiting_book),
                onClick = { onEvent(GroupSessionEvent.OnClientPicked(waiting.clientUserId)) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Small,
                state = if (canBook) ButtonState.Idle else ButtonState.Disabled,
            )
        }
    }
}

@Composable
private fun SessionActions(state: GroupSessionState, onEvent: (GroupSessionEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.isEmptySession) {
            AppButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.group_session_cancel_action),
                onClick = { onEvent(GroupSessionEvent.OnCancelClicked) },
                tone = ButtonTone.Danger,
                size = ButtonSize.Large,
                state = if (state.isResolving) ButtonState.Loading else ButtonState.Idle,
            )
        } else {
            AppButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.group_session_complete_action),
                onClick = { onEvent(GroupSessionEvent.OnCompleteClicked) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Large,
                state = if (state.isResolving) ButtonState.Loading else ButtonState.Idle,
            )
        }
        AppButton(
            text = stringResource(Res.string.group_session_add_action),
            onClick = { onEvent(GroupSessionEvent.OnAddParticipantClicked) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Large,
            state = when {
                state.isPickerLoading -> ButtonState.Loading
                state.hasFreeSeats -> ButtonState.Idle
                else -> ButtonState.Disabled
            },
        )
    }
}

@Composable
private fun MedicalBadge() {
    Box(
        modifier = Modifier
            .background(
                color = AppTheme.colors.dangerSoft,
                shape = RoundedCornerShape(AppTheme.radius.dp4),
            )
            .padding(horizontal = AppTheme.spacing.dp8, vertical = AppTheme.spacing.dp4),
    ) {
        AppText(
            text = stringResource(Res.string.people_medical_badge),
            style = AppTheme.typography.overline,
            color = AppTheme.colors.danger,
        )
    }
}
