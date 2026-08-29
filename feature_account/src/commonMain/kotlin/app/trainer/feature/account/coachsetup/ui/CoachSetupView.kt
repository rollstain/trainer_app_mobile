package app.trainer.feature.account.coachsetup.ui

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
import app.trainer.feature.account.coachsetup.mvi.CoachSetupEvent
import app.trainer.feature.account.coachsetup.mvi.CoachSetupState
import app.trainer.strings.Res
import app.trainer.strings.coach_setup_action
import app.trainer.strings.coach_setup_description
import app.trainer.strings.coach_setup_name_hint
import app.trainer.strings.coach_setup_name_label
import app.trainer.strings.coach_setup_title
import app.trainer.strings.coach_setup_zone_hint
import app.trainer.strings.coach_setup_zone_label
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
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

@Composable
fun CoachSetupView(
    modifier: Modifier = Modifier,
    state: CoachSetupState,
    onEvent: (CoachSetupEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground().imePadding()) {
        AppTopBar(
            title = stringResource(Res.string.coach_setup_title),
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
                AppText(
                    text = stringResource(Res.string.coach_setup_description),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
                AppTextField(
                    value = state.displayName,
                    onValueChange = { onEvent(CoachSetupEvent.OnDisplayNameChanged(it)) },
                    label = TextFieldLabel.Text(stringResource(Res.string.coach_setup_name_label)),
                    isEnabled = !state.isSending,
                )
                AppText(
                    text = stringResource(Res.string.coach_setup_name_hint),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textMuted,
                )
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
                        AppText(
                            text = stringResource(Res.string.coach_setup_zone_label),
                            style = AppTheme.typography.label,
                            color = AppTheme.colors.textSecondary,
                        )
                        AppText(
                            text = state.zoneId,
                            style = AppTheme.typography.bodyStrong,
                            color = AppTheme.colors.textPrimary,
                        )
                        AppText(
                            text = stringResource(Res.string.coach_setup_zone_hint),
                            style = AppTheme.typography.caption,
                            color = AppTheme.colors.textMuted,
                        )
                    }
                }
            }
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = stringResource(Res.string.coach_setup_action),
            onClick = { onEvent(CoachSetupEvent.OnStartClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSending -> ButtonState.Loading
                !state.isStartEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
    }
}
