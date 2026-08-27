package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder

enum class PlaceholderKind { Empty, Failure, NoAccess }

sealed interface PlaceholderAction {

    data object None : PlaceholderAction

    data class Button(val text: String, val onClick: () -> Unit) : PlaceholderAction
}

@Composable
fun AppStatePlaceholder(
    modifier: Modifier = Modifier,
    kind: PlaceholderKind,
    title: String,
    description: String,
    action: PlaceholderAction = PlaceholderAction.None,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.dp24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        PlaceholderIcon(kind = kind)
        Text(
            text = title,
            style = AppTheme.typography.headline,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.widthIn(max = AppTheme.sizing.placeholderTextMaxWidth),
            text = description,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        when (action) {
            PlaceholderAction.None -> Unit
            is PlaceholderAction.Button -> AppButton(
                text = action.text,
                onClick = action.onClick,
                tone = if (kind == PlaceholderKind.Empty) ButtonTone.Primary else ButtonTone.Secondary,
                size = ButtonSize.Large,
            )
        }
    }
}

@Composable
private fun PlaceholderIcon(kind: PlaceholderKind) {
    val shape = RoundedCornerShape(AppTheme.radius.dp12)
    val base = Modifier.size(AppTheme.sizing.placeholderIcon)
    when (kind) {
        PlaceholderKind.Empty, PlaceholderKind.NoAccess -> Box(
            modifier = base.dashedBorder(
                color = AppTheme.colors.borderStrong,
                cornerRadius = AppTheme.radius.dp12,
            ),
        )
        PlaceholderKind.Failure -> Box(
            modifier = base
                .background(color = AppTheme.colors.dangerSoft, shape = shape)
                .border(
                    width = AppTheme.borders.hairline,
                    color = AppTheme.colors.danger,
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                painter = AppIcons.failed,
                contentDescription = null,
                tint = AppTheme.colors.danger,
            )
        }
    }
}

@Composable
fun AppOfflineBanner(modifier: Modifier = Modifier, text: String) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizing.offlineBannerHeight)
            .background(AppTheme.colors.warningSoft)
            .padding(horizontal = AppTheme.spacing.dp16),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.warning,
        )
    }
}
