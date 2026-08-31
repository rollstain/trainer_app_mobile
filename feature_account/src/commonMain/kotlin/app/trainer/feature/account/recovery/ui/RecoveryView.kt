package app.trainer.feature.account.recovery.ui

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
import app.trainer.feature.account.recovery.mvi.RecoveryEvent
import app.trainer.feature.account.recovery.mvi.RecoveryState
import app.trainer.feature.account.recovery.mvi.RecoveryStep
import app.trainer.feature.account.telegram.TelegramLoginState
import app.trainer.strings.Res
import app.trainer.strings.recovery_cancel
import app.trainer.strings.recovery_description
import app.trainer.strings.recovery_email_label
import app.trainer.strings.recovery_failed_hint
import app.trainer.strings.recovery_failed_title
import app.trainer.strings.recovery_no_letter_hint
import app.trainer.strings.recovery_no_letter_title
import app.trainer.strings.recovery_resend_action
import app.trainer.strings.recovery_send_action
import app.trainer.strings.recovery_sending
import app.trainer.strings.recovery_sent_description
import app.trainer.strings.recovery_sent_title
import app.trainer.strings.recovery_telegram_action
import app.trainer.strings.recovery_telegram_continue
import app.trainer.strings.recovery_telegram_reopen
import app.trainer.strings.recovery_telegram_waiting_hint
import app.trainer.strings.recovery_telegram_waiting_title
import app.trainer.strings.recovery_title
import app.trainer.strings.recovery_too_soon_hint
import app.trainer.strings.recovery_too_soon_title
import app.trainer.strings.sign_in_remaining
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
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TextFieldSubmit
import org.jetbrains.compose.resources.stringResource

private const val SECONDS_IN_MINUTE = 60L
private const val COUNTDOWN_PAD = 2

@Composable
fun RecoveryView(
    modifier: Modifier = Modifier,
    state: RecoveryState,
    onEvent: (RecoveryEvent) -> Unit,
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
            contentDescription = stringResource(Res.string.recovery_title),
            onClick = onBack,
        )
        AppText(
            text = stringResource(Res.string.recovery_title),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = stringResource(Res.string.recovery_description),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.weight(1f))

        when (val step = state.step) {
            RecoveryStep.Asking -> Unit
            is RecoveryStep.LetterSent -> LetterSentBlock(secondsLeft = step.resendSecondsLeft)
            RecoveryStep.LetterRefused -> NoticeCard(
                title = stringResource(Res.string.recovery_failed_title),
                description = stringResource(Res.string.recovery_failed_hint),
                background = AppTheme.colors.dangerSoft,
                stripe = AppTheme.colors.danger,
            )
            RecoveryStep.NothingLeft -> Unit
        }
        TelegramWaiting(telegram = state.telegram, onEvent = onEvent)

        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.email,
            onValueChange = { onEvent(RecoveryEvent.OnEmailChanged(it)) },
            kind = TextFieldKind.Email,
            submit = TextFieldSubmit.Done(onSubmit = { onEvent(RecoveryEvent.OnSendClicked) }),
            label = TextFieldLabel.Text(stringResource(Res.string.recovery_email_label)),
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = when {
                state.isSending -> stringResource(Res.string.recovery_sending)
                state.step is RecoveryStep.Asking -> stringResource(Res.string.recovery_send_action)
                else -> stringResource(Res.string.recovery_resend_action)
            },
            onClick = { onEvent(RecoveryEvent.OnSendClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSending -> ButtonState.Loading
                state.isSendEnabled -> ButtonState.Idle
                else -> ButtonState.Disabled
            },
        )
        TelegramAction(state = state, onEvent = onEvent)
    }
}

@Composable
private fun LetterSentBlock(secondsLeft: Long) {
    NoticeCard(
        title = stringResource(Res.string.recovery_sent_title),
        description = stringResource(Res.string.recovery_sent_description),
        background = AppTheme.colors.successSoft,
        stripe = AppTheme.colors.success,
    )
    if (secondsLeft > RecoveryState.NO_WAIT) {
        NoticeCard(
            title = stringResource(Res.string.recovery_too_soon_title),
            description = stringResource(Res.string.recovery_too_soon_hint),
            background = AppTheme.colors.warningSoft,
            stripe = AppTheme.colors.warning,
            countdown = clockOf(secondsLeft),
        )
        return
    }
    NoticeCard(
        title = stringResource(Res.string.recovery_no_letter_title),
        description = stringResource(Res.string.recovery_no_letter_hint),
        background = AppTheme.colors.bgSurfaceSunken,
        stripe = AppTheme.colors.border,
    )
}

@Composable
private fun TelegramWaiting(telegram: TelegramLoginState, onEvent: (RecoveryEvent) -> Unit) {
    if (telegram !is TelegramLoginState.Waiting) return
    NoticeCard(
        title = stringResource(Res.string.recovery_telegram_waiting_title),
        description = stringResource(Res.string.recovery_telegram_waiting_hint),
        background = AppTheme.colors.warningSoft,
        stripe = AppTheme.colors.warning,
        countdown = clockOf(telegram.secondsLeft),
    )
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(Res.string.recovery_telegram_continue),
        onClick = { onEvent(RecoveryEvent.OnTelegramConfirmed) },
        tone = ButtonTone.Primary,
        size = ButtonSize.Large,
    )
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(Res.string.recovery_telegram_reopen),
        onClick = { onEvent(RecoveryEvent.OnTelegramClicked) },
        tone = ButtonTone.Secondary,
        size = ButtonSize.Medium,
    )
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(Res.string.recovery_cancel),
        onClick = { onEvent(RecoveryEvent.OnTelegramCancelled) },
        tone = ButtonTone.Text,
        size = ButtonSize.Medium,
    )
}

@Composable
private fun TelegramAction(state: RecoveryState, onEvent: (RecoveryEvent) -> Unit) {
    if (state.telegram is TelegramLoginState.Waiting) return
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(Res.string.recovery_telegram_action),
        onClick = { onEvent(RecoveryEvent.OnTelegramClicked) },
        tone = if (state.step is RecoveryStep.LetterRefused) ButtonTone.Primary else ButtonTone.Text,
        size = if (state.step is RecoveryStep.LetterRefused) ButtonSize.Large else ButtonSize.Medium,
        state = if (state.telegram is TelegramLoginState.Starting) ButtonState.Loading else ButtonState.Idle,
    )
}

@Composable
private fun NoticeCard(
    title: String,
    description: String,
    background: androidx.compose.ui.graphics.Color,
    stripe: androidx.compose.ui.graphics.Color,
    countdown: String? = null,
) {
    AppCard(background = background, decoration = CardDecoration.Stripe(stripe)) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            AppText(
                text = title,
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = description,
                style = AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
            )
            if (countdown != null) {
                AppText(
                    text = stringResource(Res.string.sign_in_remaining, countdown),
                    style = AppTheme.typography.numeric,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}

private fun clockOf(secondsLeft: Long): String {
    val minutes = secondsLeft / SECONDS_IN_MINUTE
    val seconds = secondsLeft % SECONDS_IN_MINUTE
    return "$minutes:${seconds.toString().padStart(COUNTDOWN_PAD, '0')}"
}
