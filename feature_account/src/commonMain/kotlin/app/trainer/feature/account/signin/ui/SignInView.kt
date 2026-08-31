package app.trainer.feature.account.signin.ui

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
import app.trainer.feature.account.signin.mvi.SignInEvent
import app.trainer.feature.account.signin.mvi.SignInFailure
import app.trainer.feature.account.signin.mvi.SignInState
import app.trainer.feature.account.telegram.TelegramLoginState
import app.trainer.strings.Res
import app.trainer.strings.sign_in_action
import app.trainer.strings.sign_in_forgot
import app.trainer.strings.sign_in_identifier_label
import app.trainer.strings.sign_in_locked_description
import app.trainer.strings.sign_in_locked_title
import app.trainer.strings.sign_in_offline
import app.trainer.strings.sign_in_password_label
import app.trainer.strings.sign_in_progress
import app.trainer.strings.sign_in_rejected
import app.trainer.strings.sign_in_remaining
import app.trainer.strings.sign_in_retry_action
import app.trainer.strings.sign_in_title
import app.trainer.strings.welcome_telegram_action
import app.trainer.strings.welcome_telegram_waiting
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppIconButton
import app.trainer.uikit.widgets.AppIcons
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

private const val SECONDS_IN_MINUTE = 60L
private const val COUNTDOWN_PAD = 2

@Composable
fun SignInView(
    modifier: Modifier = Modifier,
    state: SignInState,
    onEvent: (SignInEvent) -> Unit,
    onBack: () -> Unit,
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
                bottom = AppTheme.spacing.dp24,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        AppIconButton(
            painter = AppIcons.back,
            contentDescription = stringResource(Res.string.sign_in_title),
            onClick = onBack,
        )
        AppText(
            text = stringResource(Res.string.sign_in_title),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        Spacer(modifier = Modifier.weight(1f))

        val locked = state.failure as? SignInFailure.Locked
        if (locked != null) {
            LockedCard(secondsLeft = locked.secondsLeft)
        }

        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.identifier,
            onValueChange = { onEvent(SignInEvent.OnIdentifierChanged(it)) },
            kind = TextFieldKind.Email,
            label = TextFieldLabel.Text(stringResource(Res.string.sign_in_identifier_label)),
            message = fieldTone(state.failure),
            isEnabled = !state.isLocked,
            submit = TextFieldSubmit.Next,
        )
        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.password,
            onValueChange = { onEvent(SignInEvent.OnPasswordChanged(it)) },
            kind = TextFieldKind.Password,
            label = TextFieldLabel.Text(stringResource(Res.string.sign_in_password_label)),
            message = fieldTone(state.failure),
            action = TextFieldAction.Reveal(
                isRevealed = state.isPasswordRevealed,
                onToggle = { onEvent(SignInEvent.OnRevealToggled) },
            ),
            isEnabled = !state.isLocked,
            submit = TextFieldSubmit.Done(onSubmit = { onEvent(SignInEvent.OnSubmitClicked) }),
        )
        FailureLine(failure = state.failure)
        SubmitButton(state = state, onEvent = onEvent)
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.sign_in_forgot),
            onClick = { onEvent(SignInEvent.OnForgotClicked) },
            tone = ButtonTone.Text,
            size = ButtonSize.Medium,
        )
    }
}

@Composable
private fun LockedCard(secondsLeft: Long) {
    AppCard(
        background = AppTheme.colors.warningSoft,
        decoration = CardDecoration.Stripe(AppTheme.colors.warning),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            AppText(
                text = pluralStringResource(
                    Res.plurals.sign_in_locked_title,
                    minutesOf(secondsLeft),
                    minutesOf(secondsLeft),
                ),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = stringResource(Res.string.sign_in_locked_description),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
            )
            AppText(
                text = stringResource(Res.string.sign_in_remaining, clockOf(secondsLeft)),
                style = AppTheme.typography.numeric,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun FailureLine(failure: SignInFailure) {
    val text = when (failure) {
        SignInFailure.Rejected -> stringResource(Res.string.sign_in_rejected)
        SignInFailure.Offline -> stringResource(Res.string.sign_in_offline)
        SignInFailure.None, is SignInFailure.Locked -> return
    }
    AppText(
        text = text,
        style = AppTheme.typography.caption,
        color = if (failure == SignInFailure.Offline) AppTheme.colors.warning else AppTheme.colors.danger,
    )
}

@Composable
private fun SubmitButton(state: SignInState, onEvent: (SignInEvent) -> Unit) {
    if (state.isLocked) {
        val isWaiting = state.telegram is TelegramLoginState.Starting || state.telegram is TelegramLoginState.Waiting
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = if (isWaiting) {
                stringResource(Res.string.welcome_telegram_waiting)
            } else {
                stringResource(Res.string.welcome_telegram_action)
            },
            onClick = { onEvent(SignInEvent.OnTelegramClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = if (isWaiting) ButtonState.Loading else ButtonState.Idle,
        )
        return
    }
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = when {
            state.isSubmitting -> stringResource(Res.string.sign_in_progress)
            state.failure == SignInFailure.Offline -> stringResource(Res.string.sign_in_retry_action)
            else -> stringResource(Res.string.sign_in_action)
        },
        onClick = { onEvent(SignInEvent.OnSubmitClicked) },
        tone = ButtonTone.Primary,
        size = ButtonSize.Large,
        state = when {
            state.isSubmitting -> ButtonState.Loading
            state.isSubmitEnabled -> ButtonState.Idle
            else -> ButtonState.Disabled
        },
    )
}

private fun fieldTone(failure: SignInFailure): TextFieldMessage =
    if (failure == SignInFailure.Rejected) TextFieldMessage.Error("") else TextFieldMessage.None

private fun minutesOf(secondsLeft: Long): Int =
    ((secondsLeft + SECONDS_IN_MINUTE - 1) / SECONDS_IN_MINUTE).toInt()

private fun clockOf(secondsLeft: Long): String {
    val minutes = secondsLeft / SECONDS_IN_MINUTE
    val seconds = secondsLeft % SECONDS_IN_MINUTE
    return "$minutes:${seconds.toString().padStart(COUNTDOWN_PAD, '0')}"
}
