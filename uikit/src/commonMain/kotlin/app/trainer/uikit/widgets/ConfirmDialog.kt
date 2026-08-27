package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.trainer.uikit.AppTheme

private val DIALOG_MAX_WIDTH = 340.dp
private val DIALOG_SCREEN_MARGIN = 16.dp
private val DIALOG_PADDING = 24.dp
private val DIALOG_CONTENT_GAP = 12.dp
private val DIALOG_SHADOW = 8.dp
private const val SCRIM_ALPHA = 0.4f
private const val DESCRIPTION_MAX_LINES = 3

enum class ConfirmDialogTone { Neutral, Danger }

sealed interface ConfirmDialogDismiss {

    data object None : ConfirmDialogDismiss

    data class Action(val text: String, val onClick: () -> Unit) : ConfirmDialogDismiss
}

@Composable
fun AppConfirmDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    tone: ConfirmDialogTone = ConfirmDialogTone.Neutral,
    dismiss: ConfirmDialogDismiss = ConfirmDialogDismiss.None,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.textPrimary.copy(alpha = SCRIM_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = DIALOG_SCREEN_MARGIN)
                    .widthIn(max = DIALOG_MAX_WIDTH)
                    .shadow(
                        elevation = DIALOG_SHADOW,
                        shape = RoundedCornerShape(AppTheme.radius.dp16),
                    )
                    .background(
                        color = AppTheme.colors.bgSurface,
                        shape = RoundedCornerShape(AppTheme.radius.dp16),
                    )
                    .padding(DIALOG_PADDING),
                verticalArrangement = Arrangement.spacedBy(DIALOG_CONTENT_GAP),
            ) {
                Text(
                    text = title,
                    style = AppTheme.typography.headline,
                    color = AppTheme.colors.textPrimary,
                )
                Text(
                    text = description,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                    maxLines = DESCRIPTION_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = AppTheme.spacing.dp8,
                        alignment = Alignment.End,
                    ),
                ) {
                    when (dismiss) {
                        ConfirmDialogDismiss.None -> Unit
                        is ConfirmDialogDismiss.Action -> AppButton(
                            text = dismiss.text,
                            onClick = dismiss.onClick,
                            tone = ButtonTone.Text,
                            size = ButtonSize.Medium,
                        )
                    }
                    AppButton(
                        text = confirmText,
                        onClick = onConfirm,
                        tone = when (tone) {
                            ConfirmDialogTone.Neutral -> ButtonTone.Primary
                            ConfirmDialogTone.Danger -> ButtonTone.Danger
                        },
                        size = ButtonSize.Medium,
                    )
                }
            }
        }
    }
}
