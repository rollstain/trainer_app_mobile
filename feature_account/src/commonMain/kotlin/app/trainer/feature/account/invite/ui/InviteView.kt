package app.trainer.feature.account.invite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.invite.mvi.InviteEvent
import app.trainer.feature.account.invite.mvi.InviteState
import app.trainer.strings.Res
import app.trainer.strings.invite_case_hint
import app.trainer.strings.invite_checking_hint
import app.trainer.strings.invite_description
import app.trainer.strings.invite_expired_session_description
import app.trainer.strings.invite_expired_session_title
import app.trainer.strings.invite_submit_action
import app.trainer.strings.invite_submit_progress
import app.trainer.strings.invite_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCodeInput
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CodeInputState
import org.jetbrains.compose.resources.stringResource

@Composable
fun InviteView(
    modifier: Modifier = Modifier,
    state: InviteState,
    onEvent: (InviteEvent) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(
                start = AppTheme.spacing.dp24,
                end = AppTheme.spacing.dp24,
                top = AppTheme.spacing.dp32,
                bottom = AppTheme.spacing.dp24,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        AppText(
            text = if (state.afterSessionExpiry) {
                stringResource(Res.string.invite_expired_session_title)
            } else {
                stringResource(Res.string.invite_title)
            },
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = if (state.afterSessionExpiry) {
                stringResource(Res.string.invite_expired_session_description)
            } else {
                stringResource(Res.string.invite_description)
            },
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        AppCodeInput(
            value = state.code,
            onValueChange = { onEvent(InviteEvent.OnCodeChanged(it)) },
            state = when {
                state.isChecking -> CodeInputState.Checking
                state.codeError != null -> CodeInputState.Error
                else -> CodeInputState.Typing
            },
        )
        CodeHint(state = state)
        Spacer(modifier = Modifier.weight(1f))
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = if (state.isChecking) {
                stringResource(Res.string.invite_submit_progress)
            } else {
                stringResource(Res.string.invite_submit_action)
            },
            onClick = { onEvent(InviteEvent.OnSubmitClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isChecking -> ButtonState.Loading
                !state.isSubmitEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
    }
}

@Composable
private fun CodeHint(state: InviteState) {
    val error = state.codeError
    when {
        error != null -> AppText(
            text = error,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.danger,
        )
        state.isChecking -> AppText(
            text = stringResource(Res.string.invite_checking_hint),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
        )
        else -> AppText(
            text = stringResource(Res.string.invite_case_hint),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
    }
}
