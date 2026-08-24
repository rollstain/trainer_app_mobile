package app.trainer.feature.account.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.feature.account.profile.mvi.ProfileEvent
import app.trainer.feature.account.profile.mvi.ProfileState
import app.trainer.uikit.AppTheme
import app.trainer.uikit.leadingStripe
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.ConfirmDialogDismiss
import app.trainer.uikit.widgets.ConfirmDialogTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TopBarLeading

private const val TITLE = "Профиль"
private const val NO_CONTACT_TITLE = "Контакт не привязан"
private const val NO_CONTACT_DESCRIPTION =
    "Без него не восстановить доступ на новом устройстве."
private const val ADD_CONTACT_ACTION = "Добавить контакт"
private const val SIGN_OUT_ACTION = "Выйти из аккаунта"
private const val SIGN_OUT_TITLE = "Выйти из аккаунта?"
private const val SIGN_OUT_DESCRIPTION =
    "Переписка и записи останутся на сервере. Для входа снова понадобится код от тренера."
private const val SIGN_OUT_CONFIRM = "Выйти"
private const val SIGN_OUT_CANCEL = "Отмена"
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"
private const val CANCELLATION_TITLE = "Отмена и перенос"
private const val CANCELLATION_DESCRIPTION =
    "За сколько часов до тренировки подопечный может попросить отмену или перенос."
private val CANCELLATION_PRESETS = listOf(2, 6, 12, 24, 48)

@Composable
fun ProfileView(
    modifier: Modifier = Modifier,
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = TITLE, leading = TopBarLeading.Back(onClick = onBackClick))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            if (state.isFailed) {
                AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(ProfileEvent.OnRetryClicked) },
                    ),
                )
            } else {
                ProfileContent(state = state, onEvent = onEvent)
            }
        }
    }
    if (state.isSignOutDialogVisible) {
        AppConfirmDialog(
            title = SIGN_OUT_TITLE,
            description = SIGN_OUT_DESCRIPTION,
            confirmText = SIGN_OUT_CONFIRM,
            onConfirm = { onEvent(ProfileEvent.OnSignOutConfirmed) },
            onDismissRequest = { onEvent(ProfileEvent.OnSignOutDismissed) },
            tone = ConfirmDialogTone.Danger,
            dismiss = ConfirmDialogDismiss.Action(
                text = SIGN_OUT_CANCEL,
                onClick = { onEvent(ProfileEvent.OnSignOutDismissed) },
            ),
        )
    }
}

@Composable
private fun ProfileContent(state: ProfileState, onEvent: (ProfileEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.colors.bgSurface,
                    shape = RoundedCornerShape(AppTheme.radius.dp12),
                )
                .padding(AppTheme.spacing.dp16),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppAvatar(displayName = state.displayName, size = AvatarSize.Large)
            Column {
                AppText(
                    text = state.displayName,
                    style = AppTheme.typography.headline,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = state.contactLabel?.let { "${state.roleLabel} · $it" } ?: state.roleLabel,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        if (!state.hasContact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = AppTheme.colors.warningSoft,
                        shape = RoundedCornerShape(AppTheme.radius.dp12),
                    )
                    .leadingStripe(
                        color = AppTheme.colors.warning,
                        width = AppTheme.borders.medicalStripe,
                    )
                    .padding(AppTheme.spacing.dp16),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                AppText(
                    text = NO_CONTACT_TITLE,
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = NO_CONTACT_DESCRIPTION,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
                AppButton(
                    text = ADD_CONTACT_ACTION,
                    onClick = { onEvent(ProfileEvent.OnAddContactClicked) },
                    tone = ButtonTone.Primary,
                    size = ButtonSize.Medium,
                )
            }
        }
        if (state.cancellationWindowHours != null) {
            CancellationWindowSection(
                selectedHours = state.cancellationWindowHours,
                onSelect = { hours -> onEvent(ProfileEvent.OnCancellationWindowSelected(hours)) },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppTheme.colors.bgSurface,
                    shape = RoundedCornerShape(AppTheme.radius.dp12),
                )
                .clickable { onEvent(ProfileEvent.OnSignOutClicked) }
                .padding(AppTheme.spacing.dp16),
        ) {
            AppText(
                text = SIGN_OUT_ACTION,
                style = AppTheme.typography.body,
                color = AppTheme.colors.danger,
            )
        }
    }
}

@Composable
private fun CancellationWindowSection(selectedHours: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        AppText(
            text = CANCELLATION_TITLE,
            style = AppTheme.typography.bodyStrong,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            text = CANCELLATION_DESCRIPTION,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            CANCELLATION_PRESETS.forEach { hours ->
                AppButton(
                    text = "$hours ч",
                    onClick = { onSelect(hours) },
                    tone = if (hours == selectedHours) ButtonTone.Primary else ButtonTone.Secondary,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}
