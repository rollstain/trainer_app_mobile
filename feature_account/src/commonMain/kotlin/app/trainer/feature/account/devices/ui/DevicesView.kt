package app.trainer.feature.account.devices.ui

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
import app.trainer.feature.account.devices.mvi.DeviceRow
import app.trainer.feature.account.devices.mvi.DevicesEvent
import app.trainer.feature.account.devices.mvi.DevicesState
import app.trainer.strings.Res
import app.trainer.strings.devices_current_badge
import app.trainer.strings.devices_last_seen
import app.trainer.strings.devices_recovery_hint
import app.trainer.strings.devices_revoke_action
import app.trainer.strings.devices_revoke_others_action
import app.trainer.strings.devices_revoke_others_cancel
import app.trainer.strings.devices_revoke_others_confirm
import app.trainer.strings.devices_revoke_others_description
import app.trainer.strings.devices_revoke_others_title
import app.trainer.strings.devices_stranger_hint
import app.trainer.strings.devices_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.ConfirmDialogDismiss
import app.trainer.uikit.widgets.ConfirmDialogTone
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 2

@Composable
fun DevicesView(
    modifier: Modifier = Modifier,
    state: DevicesState,
    onEvent: (DevicesEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.devices_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(DevicesEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(
                    count = SHIMMER_CARDS,
                    lines = SHIMMER_CARD_LINES,
                )
                else -> DevicesList(state = state, onEvent = onEvent)
            }
        }
    }
    if (state.isRevokeOthersDialogVisible) {
        AppConfirmDialog(
            title = stringResource(Res.string.devices_revoke_others_title),
            description = stringResource(Res.string.devices_revoke_others_description),
            confirmText = stringResource(Res.string.devices_revoke_others_confirm),
            onConfirm = { onEvent(DevicesEvent.OnRevokeOthersConfirmed) },
            onDismissRequest = { onEvent(DevicesEvent.OnRevokeOthersDismissed) },
            tone = ConfirmDialogTone.Danger,
            dismiss = ConfirmDialogDismiss.Action(
                text = stringResource(Res.string.devices_revoke_others_cancel),
                onClick = { onEvent(DevicesEvent.OnRevokeOthersDismissed) },
            ),
        )
    }
}

@Composable
private fun DevicesList(state: DevicesState, onEvent: (DevicesEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        items(items = state.devices, key = { it.sessionId }) { device ->
            DeviceCard(
                device = device,
                isRevoking = state.revokingSessionId == device.sessionId,
                onRevoke = { onEvent(DevicesEvent.OnDeviceRevoked(device.sessionId)) },
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                AppText(
                    text = stringResource(Res.string.devices_stranger_hint),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
                AppText(
                    text = stringResource(Res.string.devices_recovery_hint),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        if (state.hasOtherDevices) {
            item {
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.devices_revoke_others_action),
                    onClick = { onEvent(DevicesEvent.OnRevokeOthersClicked) },
                    tone = ButtonTone.Danger,
                    size = ButtonSize.Large,
                    state = if (state.isRevokingOthers) ButtonState.Loading else ButtonState.Idle,
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceRow, isRevoking: Boolean, onRevoke: () -> Unit) {
    AppCard(
        decoration = if (device.isCurrent) {
            CardDecoration.Stripe(AppTheme.colors.accent)
        } else {
            CardDecoration.None
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        text = device.deviceInfo,
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    if (device.isCurrent) {
                        CurrentDeviceBadge()
                    }
                }
                AppText(
                    text = stringResource(Res.string.devices_last_seen, device.lastSeenLabel),
                    style = AppTheme.typography.numeric,
                    color = if (device.isLongUnused) AppTheme.colors.warning else AppTheme.colors.textSecondary,
                )
            }
            if (!device.isCurrent) {
                AppButton(
                    text = stringResource(Res.string.devices_revoke_action),
                    onClick = onRevoke,
                    tone = ButtonTone.Secondary,
                    size = ButtonSize.Small,
                    state = if (isRevoking) ButtonState.Loading else ButtonState.Idle,
                )
            }
        }
    }
}

@Composable
private fun CurrentDeviceBadge() {
    Box(
        modifier = Modifier
            .background(
                color = AppTheme.colors.accentSoft,
                shape = RoundedCornerShape(AppTheme.radius.dp4),
            )
            .padding(horizontal = AppTheme.spacing.dp8, vertical = AppTheme.spacing.dp4),
    ) {
        AppText(
            text = stringResource(Res.string.devices_current_badge),
            style = AppTheme.typography.overline,
            color = AppTheme.colors.accent,
        )
    }
}
