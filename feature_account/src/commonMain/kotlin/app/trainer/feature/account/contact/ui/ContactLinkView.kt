package app.trainer.feature.account.contact.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.feature.account.contact.mvi.ContactKind
import app.trainer.feature.account.contact.mvi.ContactLinkEvent
import app.trainer.feature.account.contact.mvi.ContactLinkState
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.TextFieldLabel

private const val TITLE = "Как вас найти"
private const val DESCRIPTION =
    "Контакт нужен, чтобы вернуть доступ, если смените телефон. Тренер его не видит."
private const val PHONE_TOGGLE = "Телефон"
private const val EMAIL_TOGGLE = "Почта"
private const val PHONE_LABEL = "Номер телефона"
private const val EMAIL_LABEL = "Электронная почта"
private const val SUBMIT_ACTION = "Сохранить"
private const val SKIP_ACTION = "Позже"

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
            .padding(
                start = AppTheme.spacing.dp16,
                end = AppTheme.spacing.dp16,
                top = AppTheme.spacing.dp32,
                bottom = AppTheme.spacing.dp16,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        AppText(
            text = TITLE,
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = DESCRIPTION,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppButton(
                text = PHONE_TOGGLE,
                onClick = { onEvent(ContactLinkEvent.OnKindChanged(ContactKind.Phone)) },
                tone = if (state.kind == ContactKind.Phone) {
                    ButtonTone.Primary
                } else {
                    ButtonTone.Secondary
                },
            )
            AppButton(
                text = EMAIL_TOGGLE,
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
                if (state.kind == ContactKind.Phone) PHONE_LABEL else EMAIL_LABEL
            ),
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = SUBMIT_ACTION,
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
            text = SKIP_ACTION,
            onClick = { onEvent(ContactLinkEvent.OnSkipClicked) },
            tone = ButtonTone.Text,
            size = ButtonSize.Large,
        )
    }
}
