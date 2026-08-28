package app.trainer.uikit.widgets

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val FALLBACK_MIN_HEIGHT = 72.dp

@Composable
fun AppBlockFallback(
    modifier: Modifier = Modifier,
    message: String,
    retryText: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = FALLBACK_MIN_HEIGHT)
            .border(
                width = AppTheme.borders.hairline,
                color = AppTheme.colors.border,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            modifier = Modifier.weight(1f),
            text = message,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        AppButton(
            text = retryText,
            onClick = onRetry,
            tone = ButtonTone.Secondary,
            size = ButtonSize.Small,
        )
    }
}
