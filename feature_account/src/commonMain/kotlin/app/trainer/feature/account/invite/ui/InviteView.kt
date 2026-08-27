package app.trainer.feature.account.invite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.invite.mvi.InviteEvent
import app.trainer.feature.account.invite.mvi.InviteState
import app.trainer.strings.Res
import app.trainer.strings.invite_description
import app.trainer.strings.invite_hint
import app.trainer.strings.invite_name_label
import app.trainer.strings.invite_submit_action
import app.trainer.strings.invite_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TextFieldMessage
import org.jetbrains.compose.resources.stringResource

private const val CODE_PLACEHOLDER = "AB12CD"

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
            .verticalScroll(rememberScrollState())
            .padding(
                start = AppTheme.spacing.dp16,
                end = AppTheme.spacing.dp16,
                top = AppTheme.spacing.dp32,
                bottom = AppTheme.spacing.dp16,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        AppText(
            text = stringResource(Res.string.invite_title),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = stringResource(Res.string.invite_description),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        AppTextField(
            value = state.code,
            onValueChange = { onEvent(InviteEvent.OnCodeChanged(it)) },
            kind = TextFieldKind.InviteCode,
            placeholder = CODE_PLACEHOLDER,
            message = state.codeError?.let(TextFieldMessage::Error) ?: TextFieldMessage.None,
        )
        AppTextField(
            value = state.displayName,
            onValueChange = { onEvent(InviteEvent.OnDisplayNameChanged(it)) },
            label = TextFieldLabel.Text(stringResource(Res.string.invite_name_label)),
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.invite_submit_action),
            onClick = { onEvent(InviteEvent.OnSubmitClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSubmitting -> ButtonState.Loading
                !state.isSubmitEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
        AppText(
            text = stringResource(Res.string.invite_hint),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
    }
}
