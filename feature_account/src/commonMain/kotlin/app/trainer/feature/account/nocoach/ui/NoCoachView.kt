package app.trainer.feature.account.nocoach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.nocoach.mvi.NoCoachEvent
import app.trainer.feature.account.nocoach.mvi.NoCoachState
import app.trainer.strings.Res
import app.trainer.strings.no_coach_code_hint
import app.trainer.strings.no_coach_code_title
import app.trainer.strings.no_coach_description
import app.trainer.strings.no_coach_join_action
import app.trainer.strings.no_coach_keeps_data
import app.trainer.strings.no_coach_title
import app.trainer.strings.no_coach_waiting_title
import app.trainer.strings.profile_sign_out_action
import app.trainer.strings.profile_sign_out_cancel
import app.trainer.strings.profile_sign_out_confirm
import app.trainer.strings.profile_sign_out_description
import app.trainer.strings.profile_sign_out_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCodeInput
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CodeInputState
import app.trainer.uikit.widgets.ConfirmDialogDismiss
import app.trainer.uikit.widgets.ConfirmDialogTone
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoCoachView(
    modifier: Modifier = Modifier,
    state: NoCoachState,
    onEvent: (NoCoachEvent) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = AppTheme.spacing.dp24,
                end = AppTheme.spacing.dp24,
                top = AppTheme.spacing.dp32,
                bottom = AppTheme.spacing.dp24,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        AppText(
            text = stringResource(Res.string.no_coach_title),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = stringResource(Res.string.no_coach_description),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        AppText(
            text = stringResource(Res.string.no_coach_code_title),
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        AppCodeInput(
            value = state.code,
            onValueChange = { onEvent(NoCoachEvent.OnCodeChanged(it)) },
            state = when {
                state.isJoining -> CodeInputState.Checking
                state.codeError != null -> CodeInputState.Error
                else -> CodeInputState.Typing
            },
        )
        val error = state.codeError
        AppText(
            text = error ?: stringResource(Res.string.no_coach_code_hint),
            style = AppTheme.typography.caption,
            color = if (error == null) AppTheme.colors.textMuted else AppTheme.colors.danger,
        )
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                AppText(
                    text = stringResource(Res.string.no_coach_waiting_title),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = stringResource(Res.string.no_coach_keeps_data),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.no_coach_join_action),
            onClick = { onEvent(NoCoachEvent.OnJoinClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isJoining -> ButtonState.Loading
                !state.isSubmitEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.profile_sign_out_action),
            onClick = { onEvent(NoCoachEvent.OnSignOutClicked) },
            tone = ButtonTone.Text,
            size = ButtonSize.Medium,
        )
    }
    if (state.isSignOutDialogVisible) {
        AppConfirmDialog(
            title = stringResource(Res.string.profile_sign_out_title),
            description = stringResource(Res.string.profile_sign_out_description),
            confirmText = stringResource(Res.string.profile_sign_out_confirm),
            onConfirm = { onEvent(NoCoachEvent.OnSignOutConfirmed) },
            onDismissRequest = { onEvent(NoCoachEvent.OnSignOutDismissed) },
            tone = ConfirmDialogTone.Danger,
            dismiss = ConfirmDialogDismiss.Action(
                text = stringResource(Res.string.profile_sign_out_cancel),
                onClick = { onEvent(NoCoachEvent.OnSignOutDismissed) },
            ),
        )
    }
}
