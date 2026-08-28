package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.trainer.uikit.AppTheme

enum class StatTileTone { Neutral, Success, Warning }

@Composable
fun AppStatTile(
    modifier: Modifier = Modifier,
    value: String,
    caption: String,
    tone: StatTileTone = StatTileTone.Neutral,
) {
    val colors = AppTheme.colors
    val valueColor = when (tone) {
        StatTileTone.Neutral -> colors.textPrimary
        StatTileTone.Success -> colors.success
        StatTileTone.Warning -> colors.warning
    }
    Column(
        modifier = modifier
            .background(color = colors.bgSurface, shape = RoundedCornerShape(AppTheme.radius.dp12))
            .padding(vertical = AppTheme.spacing.dp12, horizontal = AppTheme.spacing.dp8)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
        AppText(
            text = value,
            style = AppTheme.typography.numericBig,
            color = valueColor,
        )
        AppText(
            text = caption,
            style = AppTheme.typography.caption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
