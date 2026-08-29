package app.trainer.feature.account.welcome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.trainer.entities.LegalLinks
import app.trainer.feature.account.telegram.TelegramLoginState
import app.trainer.feature.account.welcome.mvi.WelcomeEvent
import app.trainer.feature.account.welcome.mvi.WelcomeState
import app.trainer.strings.Res
import app.trainer.strings.invite_expired_session_description
import app.trainer.strings.invite_expired_session_title
import app.trainer.strings.legal_privacy_link
import app.trainer.strings.legal_terms_link
import app.trainer.strings.welcome_code_action
import app.trainer.strings.welcome_consent
import app.trainer.strings.welcome_or
import app.trainer.strings.welcome_sign_in_action
import app.trainer.strings.welcome_sign_up_action
import app.trainer.strings.welcome_telegram_action
import app.trainer.strings.welcome_telegram_cancel
import app.trainer.strings.welcome_telegram_expired
import app.trainer.strings.welcome_telegram_failed
import app.trainer.strings.welcome_telegram_hint
import app.trainer.strings.welcome_telegram_waiting
import app.trainer.strings.welcome_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppLegalNote
import app.trainer.uikit.widgets.AppLogo
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.LegalLink
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeView(
    modifier: Modifier = Modifier,
    state: WelcomeState,
    legalLinks: LegalLinks,
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
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp20),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        AppLogo()
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

        TelegramFailure(telegram = state.telegram)
        ExistingAccountGroup(state = state, onEvent = onEvent)
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.welcome_sign_up_action),
            onClick = { onEvent(WelcomeEvent.OnSignUpClicked) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Large,
        )
        AppText(
            text = stringResource(Res.string.welcome_or),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.welcome_code_action),
            onClick = { onEvent(WelcomeEvent.OnCodeClicked) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Large,
        )
        ConsentNote(
            modifier = Modifier.fillMaxWidth(),
            template = Res.string.welcome_consent,
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
private fun ExistingAccountGroup(state: WelcomeState, onEvent: (WelcomeEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.welcome_sign_in_action),
            onClick = { onEvent(WelcomeEvent.OnSignInClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
        )
        TelegramButton(telegram = state.telegram, onEvent = onEvent)
    }
}

@Composable
private fun TelegramButton(telegram: TelegramLoginState, onEvent: (WelcomeEvent) -> Unit) {
    val isWaiting = telegram is TelegramLoginState.Starting || telegram is TelegramLoginState.Waiting
    AppButton(
        modifier = Modifier.fillMaxWidth(),
        text = if (isWaiting) {
            stringResource(Res.string.welcome_telegram_waiting)
        } else {
            stringResource(Res.string.welcome_telegram_action)
        },
        onClick = { onEvent(WelcomeEvent.OnTelegramClicked) },
        tone = ButtonTone.Secondary,
        size = ButtonSize.Large,
        state = if (isWaiting) ButtonState.Loading else ButtonState.Idle,
    )
    if (telegram is TelegramLoginState.Waiting) {
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

@Composable
private fun TelegramFailure(telegram: TelegramLoginState) {
    if (telegram !is TelegramLoginState.Failed) return
    AppCard(
        background = AppTheme.colors.warningSoft,
        decoration = CardDecoration.Stripe(AppTheme.colors.warning),
    ) {
        AppText(
            text = if (telegram.isExpired) {
                stringResource(Res.string.welcome_telegram_expired)
            } else {
                stringResource(Res.string.welcome_telegram_failed)
            },
            style = AppTheme.typography.body,
            color = AppTheme.colors.textPrimary,
        )
    }
}
