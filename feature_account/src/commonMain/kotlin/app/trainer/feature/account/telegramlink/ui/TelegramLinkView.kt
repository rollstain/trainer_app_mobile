package app.trainer.feature.account.telegramlink.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.telegramlink.mvi.LinkStep
import app.trainer.feature.account.telegramlink.mvi.TelegramLinkEvent
import app.trainer.feature.account.telegramlink.mvi.TelegramLinkState
import app.trainer.strings.Res
import app.trainer.strings.telegram_link_action
import app.trainer.strings.telegram_link_continue
import app.trainer.strings.telegram_link_created
import app.trainer.strings.telegram_link_description
import app.trainer.strings.telegram_link_done
import app.trainer.strings.telegram_link_done_hint
import app.trainer.strings.telegram_link_failed
import app.trainer.strings.telegram_link_failed_hint
import app.trainer.strings.telegram_link_heading
import app.trainer.strings.telegram_link_later
import app.trainer.strings.telegram_link_profile_later
import app.trainer.strings.telegram_link_retry
import app.trainer.strings.telegram_link_waiting
import app.trainer.strings.telegram_link_waiting_hint
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import org.jetbrains.compose.resources.stringResource

@Composable
fun TelegramLinkView(
    modifier: Modifier = Modifier,
    state: TelegramLinkState,
    onEvent: (TelegramLinkEvent) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                start = AppTheme.spacing.dp24,
                end = AppTheme.spacing.dp24,
                top = AppTheme.spacing.dp32,
                bottom = AppTheme.spacing.dp24,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        if (state.name.isNotEmpty()) {
            AppText(
                text = stringResource(Res.string.telegram_link_created, state.name),
                style = AppTheme.typography.label,
                color = AppTheme.colors.success,
            )
        }
        AppText(
            text = stringResource(Res.string.telegram_link_heading),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = stringResource(Res.string.telegram_link_description),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.weight(1f))

        when (state.step) {
            LinkStep.Offered -> Offer(onEvent = onEvent, isWaiting = false)
            LinkStep.Waiting -> {
                Notice(
                    title = stringResource(Res.string.telegram_link_waiting),
                    description = stringResource(Res.string.telegram_link_waiting_hint),
                    background = AppTheme.colors.warningSoft,
                    stripe = AppTheme.colors.warning,
                )
                Offer(onEvent = onEvent, isWaiting = true)
            }
            LinkStep.Linked -> {
                Notice(
                    title = stringResource(Res.string.telegram_link_done),
                    description = stringResource(Res.string.telegram_link_done_hint),
                    background = AppTheme.colors.successSoft,
                    stripe = AppTheme.colors.success,
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.telegram_link_continue),
                    onClick = { onEvent(TelegramLinkEvent.OnContinueClicked) },
                    tone = ButtonTone.Primary,
                    size = ButtonSize.Large,
                )
            }
            LinkStep.Failed -> {
                Notice(
                    title = stringResource(Res.string.telegram_link_failed),
                    description = stringResource(Res.string.telegram_link_failed_hint),
                    background = AppTheme.colors.warningSoft,
                    stripe = AppTheme.colors.warning,
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.telegram_link_retry),
                    onClick = { onEvent(TelegramLinkEvent.OnLinkClicked) },
                    tone = ButtonTone.Primary,
                    size = ButtonSize.Large,
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.telegram_link_profile_later),
                    onClick = { onEvent(TelegramLinkEvent.OnSkipClicked) },
                    tone = ButtonTone.Text,
                    size = ButtonSize.Medium,
                )
            }
        }
    }
}

@Composable
private fun Offer(onEvent: (TelegramLinkEvent) -> Unit, isWaiting: Boolean) {
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(Res.string.telegram_link_action),
        onClick = { onEvent(TelegramLinkEvent.OnLinkClicked) },
        tone = ButtonTone.Primary,
        size = ButtonSize.Large,
        state = if (isWaiting) ButtonState.Loading else ButtonState.Idle,
    )
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(Res.string.telegram_link_later),
        onClick = { onEvent(TelegramLinkEvent.OnSkipClicked) },
        tone = ButtonTone.Text,
        size = ButtonSize.Medium,
    )
}

@Composable
private fun Notice(
    title: String,
    description: String,
    background: androidx.compose.ui.graphics.Color,
    stripe: androidx.compose.ui.graphics.Color,
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
        }
    }
}
