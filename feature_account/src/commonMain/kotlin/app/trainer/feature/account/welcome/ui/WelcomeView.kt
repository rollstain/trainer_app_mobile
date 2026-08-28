package app.trainer.feature.account.welcome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trainer.data.auth.AuthProvider
import app.trainer.feature.account.welcome.mvi.TelegramLogin
import app.trainer.feature.account.welcome.mvi.WelcomeEvent
import app.trainer.feature.account.welcome.mvi.WelcomeState
import app.trainer.strings.Res
import app.trainer.strings.invite_expired_session_description
import app.trainer.strings.invite_expired_session_title
import app.trainer.strings.welcome_code_action
import app.trainer.strings.welcome_or
import app.trainer.strings.welcome_telegram_action
import app.trainer.strings.welcome_telegram_cancel
import app.trainer.strings.welcome_telegram_hint
import app.trainer.strings.welcome_telegram_waiting
import app.trainer.strings.welcome_title
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

private val LOGO_SIZE = 56.dp
private val LOGO_RADIUS = 14.dp

@Composable
fun WelcomeView(
    modifier: Modifier = Modifier,
    state: WelcomeState,
    onEvent: (WelcomeEvent) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(LOGO_SIZE)
                .background(
                    color = AppTheme.colors.accent,
                    shape = RoundedCornerShape(LOGO_RADIUS),
                ),
        )
        AppText(
            text = if (state.afterSessionExpiry) {
                stringResource(Res.string.invite_expired_session_title)
            } else {
                stringResource(Res.string.welcome_title)
            },
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        if (state.afterSessionExpiry) {
            AppText(
                text = stringResource(Res.string.invite_expired_session_description),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        val telegram = state.telegram
        if (telegram is TelegramLogin.Failed) {
            AppCard(
                background = AppTheme.colors.warningSoft,
                decoration = CardDecoration.Stripe(AppTheme.colors.warning),
            ) {
                AppText(
                    text = telegram.message,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
            }
        }
        if (state.providers.contains(AuthProvider.TELEGRAM)) {
            TelegramButton(state = state, onEvent = onEvent)
            AppText(
                text = stringResource(Res.string.welcome_or),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textMuted,
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.welcome_code_action),
            onClick = { onEvent(WelcomeEvent.OnCodeClicked) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Large,
        )
    }
}

@Composable
private fun TelegramButton(state: WelcomeState, onEvent: (WelcomeEvent) -> Unit) {
    val telegram = state.telegram
    val isWaiting = telegram is TelegramLogin.Starting || telegram is TelegramLogin.Waiting
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = if (isWaiting) {
            stringResource(Res.string.welcome_telegram_waiting)
        } else {
            stringResource(Res.string.welcome_telegram_action)
        },
        onClick = { onEvent(WelcomeEvent.OnTelegramClicked) },
        tone = ButtonTone.Primary,
        size = ButtonSize.Large,
        state = if (isWaiting) ButtonState.Loading else ButtonState.Idle,
    )
    if (telegram is TelegramLogin.Waiting) {
        AppText(
            text = stringResource(Res.string.welcome_telegram_hint),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.welcome_telegram_cancel),
            onClick = { onEvent(WelcomeEvent.OnTelegramCancelled) },
            tone = ButtonTone.Text,
            size = ButtonSize.Medium,
        )
    }
}
