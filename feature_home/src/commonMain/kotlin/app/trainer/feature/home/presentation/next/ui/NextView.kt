package app.trainer.feature.home.presentation.next.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.home.presentation.next.mvi.FillKind
import app.trainer.feature.home.presentation.next.mvi.FillRow
import app.trainer.feature.home.presentation.next.mvi.FillStatus
import app.trainer.feature.home.presentation.next.mvi.NextDynamics
import app.trainer.feature.home.presentation.next.mvi.NextEvent
import app.trainer.feature.home.presentation.next.mvi.NextHabitRow
import app.trainer.feature.home.presentation.next.mvi.NextSessionCard
import app.trainer.feature.home.presentation.next.mvi.NextState
import app.trainer.feature.home.presentation.next.mvi.PlannedToday
import app.trainer.strings.Res
import app.trainer.strings.home_profile_action
import app.trainer.strings.next_choose_time_action
import app.trainer.strings.next_dynamics_action
import app.trainer.strings.next_dynamics_empty
import app.trainer.strings.next_dynamics_title
import app.trainer.strings.next_fill_action
import app.trainer.strings.next_fill_fresh
import app.trainer.strings.next_fill_never
import app.trainer.strings.next_fill_overdue
import app.trainer.strings.next_fill_pending
import app.trainer.strings.next_fill_title
import app.trainer.strings.next_habit_by_coach
import app.trainer.strings.next_habits_title
import app.trainer.strings.next_invite_action
import app.trainer.strings.next_no_coach_description
import app.trainer.strings.next_no_coach_title
import app.trainer.strings.next_no_slots_title
import app.trainer.strings.next_not_booked_title
import app.trainer.strings.next_planned_title
import app.trainer.strings.next_request_change_action
import app.trainer.strings.next_session_overline
import app.trainer.strings.next_title
import app.trainer.strings.next_write_action
import app.trainer.strings.next_write_coach_action
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppHabitWeekBar
import app.trainer.uikit.widgets.AppLineChart
import app.trainer.uikit.widgets.AppSectionHeader
import app.trainer.uikit.widgets.AppStatusChip
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.StatusChipKind
import app.trainer.uikit.widgets.TopBarAction
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 3

@Composable
fun NextView(
    modifier: Modifier = Modifier,
    state: NextState,
    onEvent: (NextEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.next_title),
            action = TopBarAction.Avatar(
                displayName = state.clientDisplayName,
                contentDescription = stringResource(Res.string.home_profile_action),
                onClick = { onEvent(NextEvent.OnProfileClicked) },
            ),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(NextEvent.OnRetryClicked) },
                )
                else -> NextContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun NextContent(state: NextState, onEvent: (NextEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = AppTheme.spacing.dp24),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        item(key = "session") {
            Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12)) {
                SessionCard(session = state.session, onEvent = onEvent)
            }
        }
        when (val planned = state.planned) {
            PlannedToday.None -> Unit
            is PlannedToday.Workout -> {
                item(key = "planned-title") {
                    AppSectionHeader(title = stringResource(Res.string.next_planned_title))
                }
                item(key = "planned") {
                    Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
                        PlannedCard(planned = planned)
                    }
                }
            }
        }
        if (state.session !is NextSessionCard.NoCoach) {
            item(key = "fills") {
                Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
                    FillsCard(fills = state.fills, onEvent = onEvent)
                }
            }
        }
        if (state.habits.isNotEmpty()) {
            item(key = "habits-title") {
                AppSectionHeader(title = stringResource(Res.string.next_habits_title))
            }
            item(key = "habits") {
                Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
                    HabitsCard(state = state)
                }
            }
        }
        if (state.session !is NextSessionCard.NoCoach) {
            item(key = "dynamics-title") {
                AppSectionHeader(title = stringResource(Res.string.next_dynamics_title))
            }
            item(key = "dynamics") {
                Box(modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16)) {
                    DynamicsCard(dynamics = state.dynamics, onEvent = onEvent)
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: NextSessionCard, onEvent: (NextEvent) -> Unit) {
    val isToday = session is NextSessionCard.Booked && session.isToday
    AppCard(
        background = if (isToday) AppTheme.colors.accentSoft else AppTheme.colors.bgSurface,
        decoration = if (isToday) {
            CardDecoration.Stripe(color = AppTheme.colors.accent)
        } else {
            CardDecoration.None
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12)) {
            when (session) {
                NextSessionCard.NoCoach -> {
                    AppText(
                        text = stringResource(Res.string.next_no_coach_title),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = stringResource(Res.string.next_no_coach_description),
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textSecondary,
                    )
                    AppButton(
                        text = stringResource(Res.string.next_invite_action),
                        onClick = { onEvent(NextEvent.OnInviteCodeClicked) },
                        tone = ButtonTone.Primary,
                        size = ButtonSize.Large,
                    )
                }
                NextSessionCard.NoSlots -> {
                    AppText(
                        text = stringResource(Res.string.next_no_slots_title),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppButton(
                        text = stringResource(Res.string.next_write_coach_action),
                        onClick = { onEvent(NextEvent.OnChatClicked) },
                        tone = ButtonTone.Secondary,
                        size = ButtonSize.Large,
                    )
                }
                is NextSessionCard.SlotsAvailable -> {
                    AppText(
                        text = stringResource(Res.string.next_not_booked_title),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = session.summary,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textSecondary,
                    )
                    AppButton(
                        text = stringResource(Res.string.next_choose_time_action),
                        onClick = { onEvent(NextEvent.OnBookingClicked) },
                        tone = ButtonTone.Primary,
                        size = ButtonSize.Large,
                    )
                }
                is NextSessionCard.Booked -> BookedSession(session = session, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun BookedSession(session: NextSessionCard.Booked, onEvent: (NextEvent) -> Unit) {
    AppText(
        text = stringResource(Res.string.next_session_overline),
        style = AppTheme.typography.overline,
        color = AppTheme.colors.textMuted,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = session.timeLabel,
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            modifier = Modifier.weight(1f),
            text = session.dayLabel,
            style = AppTheme.typography.numeric,
            color = AppTheme.colors.textSecondary,
        )
        AppText(
            text = session.startsInLabel,
            style = AppTheme.typography.numeric,
            color = AppTheme.colors.accent,
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(displayName = session.coachDisplayName, size = AvatarSize.Small)
        AppText(
            text = session.coachDisplayName,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textPrimary,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
        if (session.canRequestChange) {
            AppButton(
                text = stringResource(Res.string.next_request_change_action),
                onClick = { onEvent(NextEvent.OnBookingClicked) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Medium,
            )
        }
        AppButton(
            text = stringResource(Res.string.next_write_action),
            onClick = { onEvent(NextEvent.OnChatClicked) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Medium,
        )
    }
}

@Composable
private fun PlannedCard(planned: PlannedToday.Workout) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12)) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
                AppText(
                    text = planned.dayTitle,
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = planned.programTitle,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textMuted,
                )
            }
            planned.exercises.forEach { exercise ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        modifier = Modifier.weight(1f),
                        text = exercise.name,
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AppText(
                        text = exercise.details,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FillsCard(fills: List<FillRow>, onEvent: (NextEvent) -> Unit) {
    AppCard {
        Column {
            AppText(
                text = stringResource(Res.string.next_fill_title),
                style = AppTheme.typography.label,
                color = AppTheme.colors.textPrimary,
            )
            fills.forEach { fill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = AppTheme.sizing.cellMedium),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
                    ) {
                        AppText(
                            text = fill.title,
                            style = AppTheme.typography.body,
                            color = AppTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        FillStatusLine(status = fill.status)
                    }
                    FillAction(fill = fill, onEvent = onEvent)
                }
            }
        }
    }
}

@Composable
private fun FillStatusLine(status: FillStatus) {
    when (status) {
        FillStatus.DoneToday -> AppStatusChip(
            text = stringResource(Res.string.next_fill_fresh),
            kind = StatusChipKind.Fresh,
        )
        FillStatus.NeverFilled -> AppText(
            text = stringResource(Res.string.next_fill_never),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
        FillStatus.Pending -> AppText(
            text = stringResource(Res.string.next_fill_pending),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
        is FillStatus.Overdue -> AppText(
            text = stringResource(Res.string.next_fill_overdue, status.days),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.warning,
        )
    }
}

@Composable
private fun FillAction(fill: FillRow, onEvent: (NextEvent) -> Unit) {
    AppButton(
        text = stringResource(Res.string.next_fill_action),
        onClick = { onEvent(NextEvent.OnFillClicked(fill.kind)) },
        tone = if (fill.status == FillStatus.DoneToday) ButtonTone.Text else ButtonTone.Secondary,
        size = ButtonSize.Medium,
    )
}

@Composable
private fun HabitsCard(state: NextState) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16)) {
            state.habits.forEach { habit ->
                HabitRow(habit = habit, weekdayLabels = state.weekdayLabels)
            }
        }
    }
}

@Composable
private fun HabitRow(habit: NextHabitRow, weekdayLabels: ImmutableList<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                modifier = Modifier.weight(1f),
                text = habit.title,
                style = AppTheme.typography.label,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (habit.isSetByCoach) {
                AppStatusChip(
                    text = stringResource(Res.string.next_habit_by_coach),
                    kind = StatusChipKind.SetByCoach,
                )
            }
            AppText(
                text = habit.doneCountLabel,
                style = AppTheme.typography.numeric,
                color = AppTheme.colors.textSecondary,
            )
        }
        AppHabitWeekBar(days = habit.days, weekdayLabels = weekdayLabels)
    }
}

@Composable
private fun DynamicsCard(dynamics: NextDynamics, onEvent: (NextEvent) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12)) {
            when (dynamics) {
                NextDynamics.NoCheckIns -> {
                    AppText(
                        text = stringResource(Res.string.next_dynamics_empty),
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textSecondary,
                    )
                    AppButton(
                        text = stringResource(Res.string.next_dynamics_action),
                        onClick = { onEvent(NextEvent.OnFillClicked(FillKind.CheckIn)) },
                        tone = ButtonTone.Secondary,
                        size = ButtonSize.Large,
                    )
                }
                is NextDynamics.Weight -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        AppText(
                            text = dynamics.valueLabel,
                            style = AppTheme.typography.numericBig,
                            color = AppTheme.colors.textPrimary,
                        )
                        AppText(
                            modifier = Modifier.weight(1f),
                            text = dynamics.dateLabel,
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.textMuted,
                        )
                        AppText(
                            text = dynamics.deltaLabel,
                            style = AppTheme.typography.numeric,
                            color = if (dynamics.isWeightDown) {
                                AppTheme.colors.success
                            } else {
                                AppTheme.colors.textSecondary
                            },
                        )
                    }
                    AppLineChart(
                        values = dynamics.values,
                        maxLabel = dynamics.maxLabel,
                        minLabel = dynamics.minLabel,
                        rangeLabel = dynamics.rangeLabel,
                    )
                    if (dynamics.measuresLabel.isNotEmpty()) {
                        AppText(
                            text = dynamics.measuresLabel,
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}
