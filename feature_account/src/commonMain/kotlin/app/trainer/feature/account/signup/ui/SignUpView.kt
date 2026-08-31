package app.trainer.feature.account.signup.ui

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
import app.trainer.entities.LegalLinks
import app.trainer.feature.account.signup.mvi.LoginField
import app.trainer.feature.account.signup.mvi.SignUpEvent
import app.trainer.feature.account.signup.mvi.SignUpState
import app.trainer.strings.Res
import app.trainer.strings.legal_privacy_link
import app.trainer.strings.legal_terms_link
import app.trainer.strings.password_chars_enough
import app.trainer.strings.password_chars_missing
import app.trainer.strings.sign_up_action
import app.trainer.strings.sign_up_consent
import app.trainer.strings.sign_up_email_label
import app.trainer.strings.sign_up_login_add
import app.trainer.strings.sign_up_login_hint
import app.trainer.strings.sign_up_login_label
import app.trainer.strings.sign_up_login_optional
import app.trainer.strings.sign_up_name_label
import app.trainer.strings.sign_up_name_placeholder
import app.trainer.strings.sign_up_password_hint
import app.trainer.strings.sign_up_password_label
import app.trainer.strings.sign_up_progress
import app.trainer.strings.sign_up_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppIconButton
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppLegalNote
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.LegalLink
import app.trainer.uikit.widgets.TextFieldAction
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TextFieldMessage
import app.trainer.uikit.widgets.TextFieldSubmit
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpView(
    modifier: Modifier = Modifier,
    state: SignUpState,
    legalLinks: LegalLinks,
    onEvent: (SignUpEvent) -> Unit,
    onBack: () -> Unit,
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
                bottom = AppTheme.spacing.dp24,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        AppIconButton(
            painter = AppIcons.back,
            contentDescription = stringResource(Res.string.sign_up_title),
            onClick = onBack,
        )
        AppText(
            text = stringResource(Res.string.sign_up_title),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        Spacer(modifier = Modifier.weight(1f))

        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.name,
            onValueChange = { onEvent(SignUpEvent.OnNameChanged(it)) },
            label = TextFieldLabel.Text(stringResource(Res.string.sign_up_name_label)),
            placeholder = stringResource(Res.string.sign_up_name_placeholder),
            submit = TextFieldSubmit.Next,
        )
        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.email,
            onValueChange = { onEvent(SignUpEvent.OnEmailChanged(it)) },
            kind = TextFieldKind.Email,
            label = TextFieldLabel.Text(stringResource(Res.string.sign_up_email_label)),
            message = state.emailError?.let(TextFieldMessage::Error) ?: TextFieldMessage.None,
            submit = TextFieldSubmit.Next,
        )
        LoginRow(state = state, onEvent = onEvent)
        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.password,
            onValueChange = { onEvent(SignUpEvent.OnPasswordChanged(it)) },
            kind = TextFieldKind.NewPassword,
            label = TextFieldLabel.Text(stringResource(Res.string.sign_up_password_label)),
            message = passwordHint(state),
            action = TextFieldAction.Reveal(
                isRevealed = state.isPasswordRevealed,
                onToggle = { onEvent(SignUpEvent.OnRevealToggled) },
            ),
            submit = TextFieldSubmit.Done(onSubmit = { onEvent(SignUpEvent.OnSubmitClicked) }),
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = if (state.isSubmitting) {
                stringResource(Res.string.sign_up_progress)
            } else {
                stringResource(Res.string.sign_up_action)
            },
            onClick = { onEvent(SignUpEvent.OnSubmitClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSubmitting -> ButtonState.Loading
                state.isSubmitEnabled -> ButtonState.Idle
                else -> ButtonState.Disabled
            },
        )
        ConsentNote(
            modifier = Modifier.fillMaxWidth(),
            template = Res.string.sign_up_consent,
            legalLinks = legalLinks,
        )
    }
}

@Composable
private fun ConsentNote(
    modifier: Modifier = Modifier,
    template: StringResource,
    legalLinks: LegalLinks,
) {
    val termsLabel = stringResource(Res.string.legal_terms_link)
    val privacyLabel = stringResource(Res.string.legal_privacy_link)
    AppLegalNote(
        modifier = modifier,
        text = stringResource(template, termsLabel, privacyLabel),
        links = persistentListOf(
            LegalLink(label = termsLabel, url = legalLinks.terms),
            LegalLink(label = privacyLabel, url = legalLinks.privacy),
        ),
    )
}

@Composable
private fun LoginRow(state: SignUpState, onEvent: (SignUpEvent) -> Unit) {
    when (val login = state.login) {
        LoginField.Hidden -> AppButton(
            text = stringResource(Res.string.sign_up_login_add),
            onClick = { onEvent(SignUpEvent.OnLoginRequested) },
            tone = ButtonTone.Text,
            size = ButtonSize.Small,
        )
        is LoginField.Shown -> AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = login.value,
            onValueChange = { onEvent(SignUpEvent.OnLoginChanged(it)) },
            label = TextFieldLabel.Text(
                stringResource(Res.string.sign_up_login_label) +
                    " · " + stringResource(Res.string.sign_up_login_optional),
            ),
            message = login.error?.let(TextFieldMessage::Error)
                ?: TextFieldMessage.Neutral(stringResource(Res.string.sign_up_login_hint)),
            action = TextFieldAction.Clear(onClear = { onEvent(SignUpEvent.OnLoginCleared) }),
            submit = TextFieldSubmit.Next,
        )
    }
}

@Composable
private fun passwordHint(state: SignUpState): TextFieldMessage = when {
    state.password.isEmpty() -> TextFieldMessage.Neutral(stringResource(Res.string.sign_up_password_hint))
    state.charsMissing > 0 -> TextFieldMessage.Warning(
        pluralStringResource(Res.plurals.password_chars_missing, state.charsMissing, state.charsMissing),
    )
    else -> TextFieldMessage.Success(
        pluralStringResource(Res.plurals.password_chars_enough, state.password.length, state.password.length),
    )
}
