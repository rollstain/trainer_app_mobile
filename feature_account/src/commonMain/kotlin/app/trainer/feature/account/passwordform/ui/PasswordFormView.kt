package app.trainer.feature.account.passwordform.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.passwordform.mvi.PasswordFormEvent
import app.trainer.feature.account.passwordform.mvi.PasswordFormState
import app.trainer.strings.Res
import app.trainer.strings.new_password_label
import app.trainer.strings.password_chars_enough
import app.trainer.strings.password_chars_missing
import app.trainer.strings.password_form_current
import app.trainer.strings.password_form_devices
import app.trainer.strings.password_form_no_email
import app.trainer.strings.password_form_save
import app.trainer.strings.password_form_title_change
import app.trainer.strings.password_form_title_set
import app.trainer.strings.sign_up_email_label
import app.trainer.strings.sign_up_password_hint
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.TextFieldAction
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TextFieldMessage
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordFormView(
    modifier: Modifier = Modifier,
    state: PasswordFormState,
    onEvent: (PasswordFormEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = if (state.hasPassword) {
                stringResource(Res.string.password_form_title_change)
            } else {
                stringResource(Res.string.password_form_title_set)
            },
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(AppTheme.spacing.dp16),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
        ) {
            if (state.needsEmail) {
                AppText(
                    text = stringResource(Res.string.password_form_no_email),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
                AppTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.email,
                    onValueChange = { onEvent(PasswordFormEvent.OnEmailChanged(it)) },
                    kind = TextFieldKind.Email,
                    label = TextFieldLabel.Text(stringResource(Res.string.sign_up_email_label)),
                    message = state.emailError?.let(TextFieldMessage::Error) ?: TextFieldMessage.None,
                )
            }
            if (state.hasPassword) {
                AppTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.currentPassword,
                    onValueChange = { onEvent(PasswordFormEvent.OnCurrentPasswordChanged(it)) },
                    kind = TextFieldKind.Password,
                    label = TextFieldLabel.Text(stringResource(Res.string.password_form_current)),
                    message = state.currentPasswordError?.let(TextFieldMessage::Error) ?: TextFieldMessage.None,
                )
            }
            AppTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.newPassword,
                onValueChange = { onEvent(PasswordFormEvent.OnNewPasswordChanged(it)) },
                kind = TextFieldKind.NewPassword,
                label = TextFieldLabel.Text(stringResource(Res.string.new_password_label)),
                message = passwordHint(state),
                action = TextFieldAction.Reveal(
                    isRevealed = state.isRevealed,
                    onToggle = { onEvent(PasswordFormEvent.OnRevealToggled) },
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            AppCard(
                background = AppTheme.colors.warningSoft,
                decoration = CardDecoration.Stripe(AppTheme.colors.warning),
            ) {
                AppText(
                    text = stringResource(Res.string.password_form_devices),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
            }
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.password_form_save),
                onClick = { onEvent(PasswordFormEvent.OnSaveClicked) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Large,
                state = when {
                    state.isSaving -> ButtonState.Loading
                    state.isSaveEnabled -> ButtonState.Idle
                    else -> ButtonState.Disabled
                },
            )
        }
    }
}

@Composable
private fun passwordHint(state: PasswordFormState): TextFieldMessage = when {
    state.newPassword.isEmpty() -> TextFieldMessage.Neutral(stringResource(Res.string.sign_up_password_hint))
    state.charsMissing > 0 -> TextFieldMessage.Warning(
        pluralStringResource(Res.plurals.password_chars_missing, state.charsMissing, state.charsMissing),
    )
    else -> TextFieldMessage.Success(
        pluralStringResource(Res.plurals.password_chars_enough, state.newPassword.length, state.newPassword.length),
    )
}
