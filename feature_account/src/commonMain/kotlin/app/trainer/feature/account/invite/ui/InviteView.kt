package app.trainer.feature.account.invite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.invite.mvi.InviteEvent
import app.trainer.feature.account.invite.mvi.InviteState
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

private const val TITLE = "Вход по коду"
private const val DESCRIPTION =
    "Введите код из шести символов — тренер прислал его в сообщении."
private const val CODE_PLACEHOLDER = "AB12CD"
private const val NAME_LABEL = "Как вас зовут"
private const val SUBMIT_ACTION = "Продолжить"
private const val HINT = "Если открыть ссылку от тренера, код подставится сам."

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
            .padding(
                start = AppTheme.spacing.dp16,
                end = AppTheme.spacing.dp16,
                top = AppTheme.spacing.dp32,
                bottom = AppTheme.spacing.dp16,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        AppText(
            text = TITLE,
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = DESCRIPTION,
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
            label = TextFieldLabel.Text(NAME_LABEL),
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = SUBMIT_ACTION,
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
            text = HINT,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
    }
}
