package app.trainer.feature.account.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.data.clients.CoachPolicy
import app.trainer.feature.account.profile.mvi.ProfileEvent
import app.trainer.feature.account.profile.mvi.ProfileState
import app.trainer.strings.Res
import app.trainer.strings.coaches_title
import app.trainer.strings.login_methods_title
import app.trainer.strings.profile_add_contact_action
import app.trainer.strings.profile_become_coach_action
import app.trainer.strings.profile_cancellation_description
import app.trainer.strings.profile_cancellation_hours
import app.trainer.strings.profile_cancellation_title
import app.trainer.strings.profile_devices_action
import app.trainer.strings.profile_exercise_library_action
import app.trainer.strings.profile_management_section
import app.trainer.strings.profile_no_contact_description
import app.trainer.strings.profile_no_contact_title
import app.trainer.strings.profile_programs_action
import app.trainer.strings.profile_reminders_check_in
import app.trainer.strings.profile_reminders_description
import app.trainer.strings.profile_reminders_diary
import app.trainer.strings.profile_reminders_hour
import app.trainer.strings.profile_reminders_hour_description
import app.trainer.strings.profile_reminders_hour_title
import app.trainer.strings.profile_reminders_session
import app.trainer.strings.profile_reminders_title
import app.trainer.strings.profile_rest_description
import app.trainer.strings.profile_rest_title
import app.trainer.strings.profile_sign_out_action
import app.trainer.strings.profile_sign_out_cancel
import app.trainer.strings.profile_sign_out_confirm
import app.trainer.strings.profile_sign_out_description
import app.trainer.strings.profile_sign_out_title
import app.trainer.strings.profile_title
import app.trainer.strings.profile_working_hours_not_set
import app.trainer.strings.profile_working_hours_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppSectionHeader
import app.trainer.uikit.widgets.AppSwitch
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardAction
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.ConfirmDialogDismiss
import app.trainer.uikit.widgets.ConfirmDialogTone
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 2
private val CANCELLATION_PRESETS = listOf(2, 6, 12, 24, 48)
private val REMINDER_HOUR_PRESETS = listOf(7, 8, 9, 10, 12, 18, 20)
private val REST_PRESETS_SECONDS = listOf(60, 90, 120, 150, 180, 240)
private const val SECONDS_IN_MINUTE = 60
private const val SECONDS_DIGITS = 2

@Composable
fun ProfileView(
    modifier: Modifier = Modifier,
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.profile_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ProfileEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(
                    count = SHIMMER_CARDS,
                    lines = SHIMMER_CARD_LINES,
                )
                else -> ProfileContent(state = state, onEvent = onEvent)
            }
        }
    }
    if (state.isSignOutDialogVisible) {
        AppConfirmDialog(
            title = stringResource(Res.string.profile_sign_out_title),
            description = stringResource(Res.string.profile_sign_out_description),
            confirmText = stringResource(Res.string.profile_sign_out_confirm),
            onConfirm = { onEvent(ProfileEvent.OnSignOutConfirmed) },
            onDismissRequest = { onEvent(ProfileEvent.OnSignOutDismissed) },
            tone = ConfirmDialogTone.Danger,
            dismiss = ConfirmDialogDismiss.Action(
                text = stringResource(Res.string.profile_sign_out_cancel),
                onClick = { onEvent(ProfileEvent.OnSignOutDismissed) },
            ),
        )
    }
}

@Composable
private fun WorkingHoursCard(label: String?, onEvent: (ProfileEvent) -> Unit) {
    AppCard(action = CardAction.Click { onEvent(ProfileEvent.OnWorkingHoursClicked) }) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            AppText(
                text = stringResource(Res.string.profile_working_hours_title),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = label ?: stringResource(Res.string.profile_working_hours_not_set),
                style = AppTheme.typography.caption,
                color = if (label != null) AppTheme.colors.textSecondary else AppTheme.colors.warning,
            )
        }
    }
}

@Composable
private fun ProfileContent(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppAvatar(displayName = state.displayName, size = AvatarSize.Large)
                Column {
                    AppText(
                        text = state.displayName,
                        style = AppTheme.typography.headline,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = state.contactLabel?.let { "${state.roleLabel} · $it" } ?: state.roleLabel,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
        }
        if (!state.hasContact) {
            AppCard(
                background = AppTheme.colors.warningSoft,
                decoration = CardDecoration.Stripe(AppTheme.colors.warning),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                    AppText(
                        text = stringResource(Res.string.profile_no_contact_title),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = stringResource(Res.string.profile_no_contact_description),
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textSecondary,
                    )
                    AppButton(
                        text = stringResource(Res.string.profile_add_contact_action),
                        onClick = { onEvent(ProfileEvent.OnAddContactClicked) },
                        tone = ButtonTone.Primary,
                        size = ButtonSize.Medium,
                    )
                }
            }
        }
        if (state.policy != null) {
            WorkingHoursCard(label = state.workingHoursLabel, onEvent = onEvent)
            CancellationWindowSection(
                selectedHours = state.policy.cancellationWindowHours,
                onSelect = { hours -> onEvent(ProfileEvent.OnCancellationWindowSelected(hours)) },
            )
            RemindersSection(policy = state.policy, onEvent = onEvent)
        }
        if (state.isCoach) {
            AppCard(action = CardAction.Click { onEvent(ProfileEvent.OnProgramsClicked) }) {
                AppText(
                    text = stringResource(Res.string.profile_programs_action),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
            }
        }
        AppCard(
            action = CardAction.Click { onEvent(ProfileEvent.OnExerciseLibraryClicked) },
        ) {
            AppText(
                text = stringResource(Res.string.profile_exercise_library_action),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
            )
        }
        if (!state.isCoach) {
            RestSection(
                selectedSeconds = state.restSeconds,
                onSelect = { seconds -> onEvent(ProfileEvent.OnRestSecondsSelected(seconds)) },
            )
        }
        if (!state.isCoach) {
            AppCard(action = CardAction.Click { onEvent(ProfileEvent.OnBecomeCoachClicked) }) {
                AppText(
                    text = stringResource(Res.string.profile_become_coach_action),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
            }
        }
        if (state.isOwner) {
            AppSectionHeader(title = stringResource(Res.string.profile_management_section))
            AppCard(action = CardAction.Click { onEvent(ProfileEvent.OnCoachesClicked) }) {
                AppText(
                    text = stringResource(Res.string.coaches_title),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
            }
        }
        AppCard(action = CardAction.Click { onEvent(ProfileEvent.OnLoginMethodsClicked) }) {
            AppText(
                text = stringResource(Res.string.login_methods_title),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
            )
        }
        AppCard(action = CardAction.Click { onEvent(ProfileEvent.OnDevicesClicked) }) {
            AppText(
                text = stringResource(Res.string.profile_devices_action),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
            )
        }
        AppCard(action = CardAction.Click { onEvent(ProfileEvent.OnSignOutClicked) }) {
            AppText(
                text = stringResource(Res.string.profile_sign_out_action),
                style = AppTheme.typography.body,
                color = AppTheme.colors.danger,
            )
        }
    }
}

@Composable
private fun CancellationWindowSection(selectedHours: Int, onSelect: (Int) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppText(
                text = stringResource(Res.string.profile_cancellation_title),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = stringResource(Res.string.profile_cancellation_description),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                items(items = CANCELLATION_PRESETS, key = { it }) { hours ->
                    AppButton(
                        text = stringResource(Res.string.profile_cancellation_hours, hours),
                        onClick = { onSelect(hours) },
                        tone = if (hours == selectedHours) ButtonTone.Primary else ButtonTone.Secondary,
                        size = ButtonSize.Small,
                    )
                }
            }
        }
    }
}

@Composable
private fun RemindersSection(policy: CoachPolicy, onEvent: (ProfileEvent) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppText(
                text = stringResource(Res.string.profile_reminders_title),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = stringResource(Res.string.profile_reminders_description),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            ReminderToggle(
                title = stringResource(Res.string.profile_reminders_session),
                checked = policy.sessionRemindersEnabled,
                onCheckedChange = { onEvent(ProfileEvent.OnSessionRemindersToggled(it)) },
            )
            ReminderToggle(
                title = stringResource(Res.string.profile_reminders_diary),
                checked = policy.diaryRemindersEnabled,
                onCheckedChange = { onEvent(ProfileEvent.OnDiaryRemindersToggled(it)) },
            )
            ReminderToggle(
                title = stringResource(Res.string.profile_reminders_check_in),
                checked = policy.checkInRemindersEnabled,
                onCheckedChange = { onEvent(ProfileEvent.OnCheckInRemindersToggled(it)) },
            )
            AppText(
                text = stringResource(Res.string.profile_reminders_hour_title),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = stringResource(Res.string.profile_reminders_hour_description),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                items(items = REMINDER_HOUR_PRESETS, key = { it }) { hour ->
                    AppButton(
                        text = stringResource(Res.string.profile_reminders_hour, hour),
                        onClick = { onEvent(ProfileEvent.OnReminderHourSelected(hour)) },
                        tone = if (hour == policy.reminderHour) ButtonTone.Primary else ButtonTone.Secondary,
                        size = ButtonSize.Small,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textPrimary,
        )
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RestSection(selectedSeconds: Int, onSelect: (Int) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppText(
                text = stringResource(Res.string.profile_rest_title),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = stringResource(Res.string.profile_rest_description),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                items(items = REST_PRESETS_SECONDS, key = { it }) { seconds ->
                    AppButton(
                        text = restLabelOf(seconds),
                        onClick = { onSelect(seconds) },
                        tone = if (seconds == selectedSeconds) ButtonTone.Primary else ButtonTone.Secondary,
                        size = ButtonSize.Small,
                    )
                }
            }
        }
    }
}

private fun restLabelOf(seconds: Int): String {
    val minutes = seconds / SECONDS_IN_MINUTE
    val rest = seconds % SECONDS_IN_MINUTE
    return "$minutes:${rest.toString().padStart(SECONDS_DIGITS, '0')}"
}
