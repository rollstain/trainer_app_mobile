package app.trainer.feature.account.onboarding.ui

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
import app.trainer.feature.account.onboarding.mvi.DISPLAY_NAME_MAX_LENGTH
import app.trainer.feature.account.onboarding.mvi.OnboardingEvent
import app.trainer.feature.account.onboarding.mvi.OnboardingState
import app.trainer.strings.Res
import app.trainer.strings.onboarding_continue_action
import app.trainer.strings.onboarding_counter
import app.trainer.strings.onboarding_description
import app.trainer.strings.onboarding_name_label
import app.trainer.strings.onboarding_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TextFieldMessage
import org.jetbrains.compose.resources.stringResource

private const val COUNTER_SHOWN_FROM = 35

@Composable
fun OnboardingView(
    modifier: Modifier = Modifier,
    state: OnboardingState,
    onEvent: (OnboardingEvent) -> Unit,
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
        AppText(
            text = stringResource(Res.string.onboarding_title),
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = stringResource(Res.string.onboarding_description),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        AppTextField(
            value = state.displayName,
            onValueChange = { onEvent(OnboardingEvent.OnDisplayNameChanged(it)) },
            label = TextFieldLabel.Text(stringResource(Res.string.onboarding_name_label)),
            message = state.nameError?.let(TextFieldMessage::Error) ?: TextFieldMessage.None,
            isEnabled = !state.isSaving,
        )
        if (state.displayName.length >= COUNTER_SHOWN_FROM) {
            AppText(
                text = stringResource(
                    Res.string.onboarding_counter,
                    state.displayName.length,
                    DISPLAY_NAME_MAX_LENGTH,
                ),
                style = AppTheme.typography.caption,
                color = if (state.displayName.length > DISPLAY_NAME_MAX_LENGTH) {
                    AppTheme.colors.danger
                } else {
                    AppTheme.colors.textMuted
                },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.onboarding_continue_action),
            onClick = { onEvent(OnboardingEvent.OnContinueClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = if (state.isSaving) ButtonState.Loading else ButtonState.Idle,
        )
    }
}
