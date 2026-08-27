package app.trainer.feature.account.contact.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.contact.mvi.ContactKind
import app.trainer.feature.account.contact.mvi.ContactLinkEvent
import app.trainer.feature.account.contact.mvi.ContactLinkState
import app.trainer.strings.Res
import app.trainer.strings.contact_link_description
import app.trainer.strings.contact_link_email_label
import app.trainer.strings.contact_link_email_toggle
import app.trainer.strings.contact_link_phone_label
import app.trainer.strings.contact_link_phone_toggle
import app.trainer.strings.contact_link_skip_action
import app.trainer.strings.contact_link_submit_action
import app.trainer.strings.contact_link_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.TextFieldLabel
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContactLinkView(
    modifier: Modifier = Modifier,
    state: ContactLinkState,
    onEvent: (ContactLinkEvent) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = AppTheme.spacing.dp16,
                end = AppTheme.spacing.dp16,
                top = AppTheme.spacing.dp32,
                bottom = AppTheme.spacing.dp16,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        AppText(
            text = stringResource(Res.string.contact_link_title),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = stringResource(Res.string.contact_link_description),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppButton(
                text = stringResource(Res.string.contact_link_phone_toggle),
                onClick = { onEvent(ContactLinkEvent.OnKindChanged(ContactKind.Phone)) },
                tone = if (state.kind == ContactKind.Phone) {
                    ButtonTone.Primary
                } else {
                    ButtonTone.Secondary
                },
            )
            AppButton(
                text = stringResource(Res.string.contact_link_email_toggle),
                onClick = { onEvent(ContactLinkEvent.OnKindChanged(ContactKind.Email)) },
                tone = if (state.kind == ContactKind.Email) {
                    ButtonTone.Primary
                } else {
                    ButtonTone.Secondary
                },
            )
        }
        AppTextField(
            value = state.value,
            onValueChange = { onEvent(ContactLinkEvent.OnValueChanged(it)) },
            label = TextFieldLabel.Text(
                if (state.kind == ContactKind.Phone) {
                    stringResource(Res.string.contact_link_phone_label)
                } else {
                    stringResource(Res.string.contact_link_email_label)
                }
            ),
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.contact_link_submit_action),
            onClick = { onEvent(ContactLinkEvent.OnSubmitClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSubmitting -> ButtonState.Loading
                !state.isSubmitEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.contact_link_skip_action),
            onClick = { onEvent(ContactLinkEvent.OnSkipClicked) },
            tone = ButtonTone.Text,
            size = ButtonSize.Large,
        )
    }
}
