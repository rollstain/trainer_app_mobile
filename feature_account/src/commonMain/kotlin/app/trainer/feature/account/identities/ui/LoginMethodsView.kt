package app.trainer.feature.account.identities.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.account.identities.mvi.LinkProgress
import app.trainer.feature.account.identities.mvi.LoginMethodRow
import app.trainer.feature.account.identities.mvi.LoginMethodsEvent
import app.trainer.feature.account.identities.mvi.LoginMethodsState
import app.trainer.feature.account.providers.providerNameOf
import app.trainer.strings.Res
import app.trainer.strings.login_methods_email
import app.trainer.strings.login_methods_email_change
import app.trainer.strings.login_methods_empty
import app.trainer.strings.login_methods_last_hint
import app.trainer.strings.login_methods_link_telegram
import app.trainer.strings.login_methods_password
import app.trainer.strings.login_methods_password_absent
import app.trainer.strings.login_methods_password_change
import app.trainer.strings.login_methods_password_set
import app.trainer.strings.login_methods_title
import app.trainer.strings.login_methods_unlink
import app.trainer.strings.login_methods_unlink_cancel
import app.trainer.strings.login_methods_unlink_confirm
import app.trainer.strings.login_methods_unlink_description
import app.trainer.strings.login_methods_unlink_title
import app.trainer.strings.login_methods_why
import app.trainer.strings.welcome_telegram_cancel
import app.trainer.strings.welcome_telegram_hint
import app.trainer.strings.welcome_telegram_waiting
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.ConfirmDialogDismiss
import app.trainer.uikit.widgets.ConfirmDialogTone
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 2
private const val SHIMMER_CARD_LINES = 2

@Composable
fun LoginMethodsView(
    modifier: Modifier = Modifier,
    state: LoginMethodsState,
    onEvent: (LoginMethodsEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.login_methods_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(LoginMethodsEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                else -> MethodsContent(state = state, onEvent = onEvent)
            }
        }
    }
    if (state.confirmedUnlink != null) {
        AppConfirmDialog(
            title = stringResource(Res.string.login_methods_unlink_title),
            description = stringResource(Res.string.login_methods_unlink_description),
            confirmText = stringResource(Res.string.login_methods_unlink_confirm),
            onConfirm = { onEvent(LoginMethodsEvent.OnUnlinkConfirmed) },
            onDismissRequest = { onEvent(LoginMethodsEvent.OnUnlinkDismissed) },
            tone = ConfirmDialogTone.Danger,
            dismiss = ConfirmDialogDismiss.Action(
                text = stringResource(Res.string.login_methods_unlink_cancel),
                onClick = { onEvent(LoginMethodsEvent.OnUnlinkDismissed) },
            ),
        )
    }
}

@Composable
private fun PasswordCard(state: LoginMethodsState, onEvent: (LoginMethodsEvent) -> Unit) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = stringResource(Res.string.login_methods_password),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = state.passwordChangedLabel
                        ?: stringResource(Res.string.login_methods_password_absent),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
            AppButton(
                text = if (state.hasPassword) {
                    stringResource(Res.string.login_methods_password_change)
                } else {
                    stringResource(Res.string.login_methods_password_set)
                },
                onClick = { onEvent(LoginMethodsEvent.OnPasswordClicked) },
                tone = if (state.hasPassword) ButtonTone.Secondary else ButtonTone.Primary,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun EmailCard(state: LoginMethodsState, onEvent: (LoginMethodsEvent) -> Unit) {
    val email = state.email ?: return
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = stringResource(Res.string.login_methods_email),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = email,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
            AppButton(
                text = stringResource(Res.string.login_methods_email_change),
                onClick = { onEvent(LoginMethodsEvent.OnEmailClicked) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun MethodsContent(state: LoginMethodsState, onEvent: (LoginMethodsEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        AppText(
            text = stringResource(Res.string.login_methods_why),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        if (state.methods.isEmpty()) {
            AppText(
                text = stringResource(Res.string.login_methods_empty),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textMuted,
            )
        }
        state.methods.forEach { method ->
            MethodCard(
                method = method,
                isLastMethod = state.isLastMethod,
                isUnlinking = state.unlinking == method.provider,
                onEvent = onEvent,
            )
        }
        PasswordCard(state = state, onEvent = onEvent)
        EmailCard(state = state, onEvent = onEvent)
        val link = state.link
        if (link is LinkProgress.Failed) {
            AppCard(
                background = AppTheme.colors.warningSoft,
                decoration = CardDecoration.Stripe(AppTheme.colors.warning),
            ) {
                AppText(
                    text = link.message,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
            }
        }
        if (state.canLinkTelegram) {
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (link is LinkProgress.Waiting) {
                    stringResource(Res.string.welcome_telegram_waiting)
                } else {
                    stringResource(Res.string.login_methods_link_telegram)
                },
                onClick = { onEvent(LoginMethodsEvent.OnLinkTelegramClicked) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Large,
                state = if (link is LinkProgress.Waiting) ButtonState.Loading else ButtonState.Idle,
            )
        }
        if (link is LinkProgress.Waiting) {
            AppText(
                text = stringResource(Res.string.welcome_telegram_hint),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.welcome_telegram_cancel),
                onClick = { onEvent(LoginMethodsEvent.OnLinkCancelled) },
                tone = ButtonTone.Text,
                size = ButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun MethodCard(
    method: LoginMethodRow,
    isLastMethod: Boolean,
    isUnlinking: Boolean,
    onEvent: (LoginMethodsEvent) -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = providerNameOf(method.provider),
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = method.linkedAtLabel,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
                if (!isLastMethod) {
                    AppButton(
                        text = stringResource(Res.string.login_methods_unlink),
                        onClick = { onEvent(LoginMethodsEvent.OnUnlinkClicked(method.provider)) },
                        tone = ButtonTone.Secondary,
                        size = ButtonSize.Small,
                        state = if (isUnlinking) ButtonState.Loading else ButtonState.Idle,
                    )
                }
            }
            if (isLastMethod) {
                AppText(
                    text = stringResource(Res.string.login_methods_last_hint),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textMuted,
                )
            }
        }
    }
}
