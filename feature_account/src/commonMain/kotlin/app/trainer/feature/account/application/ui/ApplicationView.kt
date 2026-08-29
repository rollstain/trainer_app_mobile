package app.trainer.feature.account.application.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.feature.account.application.mvi.ABOUT_MAX_LENGTH
import app.trainer.feature.account.application.mvi.ApplicationEvent
import app.trainer.feature.account.application.mvi.ApplicationState
import app.trainer.strings.Res
import app.trainer.strings.application_about_counter
import app.trainer.strings.application_about_hint
import app.trainer.strings.application_about_label
import app.trainer.strings.application_about_placeholder
import app.trainer.strings.application_about_required
import app.trainer.strings.application_about_too_short
import app.trainer.strings.application_name_hint
import app.trainer.strings.application_name_label
import app.trainer.strings.application_send_action
import app.trainer.strings.application_title
import app.trainer.strings.application_why_description
import app.trainer.strings.application_why_title
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
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TextFieldMessage
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

@Composable
fun ApplicationView(
    modifier: Modifier = Modifier,
    state: ApplicationState,
    onEvent: (ApplicationEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground().imePadding()) {
        AppTopBar(
            title = stringResource(Res.string.application_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.spacing.dp16),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
            ) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                        AppText(
                            text = stringResource(Res.string.application_why_title),
                            style = AppTheme.typography.bodyStrong,
                            color = AppTheme.colors.textPrimary,
                        )
                        AppText(
                            text = stringResource(Res.string.application_why_description),
                            style = AppTheme.typography.body,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                }
                AppTextField(
                    value = state.displayName,
                    onValueChange = { onEvent(ApplicationEvent.OnDisplayNameChanged(it)) },
                    label = TextFieldLabel.Text(stringResource(Res.string.application_name_label)),
                    isEnabled = !state.isSending,
                )
                AppText(
                    text = stringResource(Res.string.application_name_hint),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textMuted,
                )
                AppText(
                    text = stringResource(Res.string.application_about_required),
                    style = AppTheme.typography.label,
                    color = AppTheme.colors.textSecondary,
                )
                AppTextField(
                    value = state.about,
                    onValueChange = { onEvent(ApplicationEvent.OnAboutChanged(it)) },
                    kind = TextFieldKind.Multiline,
                    label = TextFieldLabel.Text(stringResource(Res.string.application_about_label)),
                    placeholder = stringResource(Res.string.application_about_placeholder),
                    message = if (state.isTooShortShown) {
                        TextFieldMessage.Error(stringResource(Res.string.application_about_too_short))
                    } else {
                        TextFieldMessage.None
                    },
                    isEnabled = !state.isSending,
                )
                AppText(
                    text = if (state.isAboutCounterVisible) {
                        stringResource(Res.string.application_about_counter, state.about.length, ABOUT_MAX_LENGTH)
                    } else {
                        stringResource(Res.string.application_about_hint)
                    },
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textMuted,
                )
            }
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = stringResource(Res.string.application_send_action),
            onClick = { onEvent(ApplicationEvent.OnSendClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSending -> ButtonState.Loading
                !state.isSendEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
    }
}
