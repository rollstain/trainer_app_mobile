package app.trainer.feature.account.newpassword.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.newpassword.mvi.LinkState
import app.trainer.feature.account.newpassword.mvi.NewPasswordEvent
import app.trainer.feature.account.newpassword.mvi.NewPasswordState
import app.trainer.strings.Res
import app.trainer.strings.link_expired_description
import app.trainer.strings.link_expired_title
import app.trainer.strings.link_request_new
import app.trainer.strings.link_used_description
import app.trainer.strings.link_used_title
import app.trainer.strings.new_password_action
import app.trainer.strings.new_password_description
import app.trainer.strings.new_password_devices_warning
import app.trainer.strings.new_password_label
import app.trainer.strings.new_password_title_unknown
import app.trainer.strings.password_chars_enough
import app.trainer.strings.password_chars_missing
import app.trainer.strings.recovery_not_confirmed
import app.trainer.strings.sign_up_password_hint
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.TextFieldAction
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TextFieldMessage
import app.trainer.uikit.widgets.TextFieldSubmit
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NewPasswordView(
    modifier: Modifier = Modifier,
    state: NewPasswordState,
    onEvent: (NewPasswordEvent) -> Unit,
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
        if (state.link == LinkState.AlreadyUsed || state.link == LinkState.Expired) {
            DeadLink(state = state, onEvent = onEvent)
            return@Column
        }

        AppText(
            text = stringResource(Res.string.new_password_title_unknown),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = stringResource(Res.string.new_password_description),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.weight(1f))

        if (state.link == LinkState.NotConfirmedYet) {
            AppCard(
                background = AppTheme.colors.warningSoft,
                decoration = CardDecoration.Stripe(AppTheme.colors.warning),
            ) {
                AppText(
                    text = stringResource(Res.string.recovery_not_confirmed),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
            }
        }

        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.password,
            onValueChange = { onEvent(NewPasswordEvent.OnPasswordChanged(it)) },
            kind = TextFieldKind.NewPassword,
            submit = TextFieldSubmit.Done(onSubmit = { onEvent(NewPasswordEvent.OnSubmitClicked) }),
            label = TextFieldLabel.Text(stringResource(Res.string.new_password_label)),
            message = passwordHint(state),
            action = TextFieldAction.Reveal(
                isRevealed = state.isRevealed,
                onToggle = { onEvent(NewPasswordEvent.OnRevealToggled) },
            ),
        )
        AppCard(
            background = AppTheme.colors.warningSoft,
            decoration = CardDecoration.Stripe(AppTheme.colors.warning),
        ) {
            AppText(
                text = stringResource(Res.string.new_password_devices_warning),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.new_password_action),
            onClick = { onEvent(NewPasswordEvent.OnSubmitClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSubmitting -> ButtonState.Loading
                state.isSubmitEnabled -> ButtonState.Idle
                else -> ButtonState.Disabled
            },
        )
    }
}

@Composable
private fun ColumnScope.DeadLink(state: NewPasswordState, onEvent: (NewPasswordEvent) -> Unit) {
    val isUsed = state.link == LinkState.AlreadyUsed
    Spacer(modifier = Modifier.weight(1f))
    AppText(
        text = if (isUsed) {
            stringResource(Res.string.link_used_title)
        } else {
            stringResource(Res.string.link_expired_title)
        },
        style = AppTheme.typography.display,
        color = AppTheme.colors.textPrimary,
    )
    AppText(
        text = if (isUsed) {
            stringResource(Res.string.link_used_description)
        } else {
            stringResource(Res.string.link_expired_description)
        },
        style = AppTheme.typography.body,
        color = AppTheme.colors.textSecondary,
    )
    Spacer(modifier = Modifier.weight(1f))
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(Res.string.link_request_new),
        onClick = { onEvent(NewPasswordEvent.OnRequestNewLinkClicked) },
        tone = ButtonTone.Primary,
        size = ButtonSize.Large,
    )
}

@Composable
private fun passwordHint(state: NewPasswordState): TextFieldMessage = when {
    state.password.isEmpty() -> TextFieldMessage.Neutral(stringResource(Res.string.sign_up_password_hint))
    state.charsMissing > 0 -> TextFieldMessage.Warning(
        pluralStringResource(Res.plurals.password_chars_missing, state.charsMissing, state.charsMissing),
    )
    else -> TextFieldMessage.Success(
        pluralStringResource(Res.plurals.password_chars_enough, state.password.length, state.password.length),
    )
}
