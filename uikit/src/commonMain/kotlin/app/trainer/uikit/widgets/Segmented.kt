package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val TRACK_HEIGHT = 40.dp
private val TRACK_PADDING = 2.dp
private val SEGMENT_RADIUS = 6.dp
private const val SEGMENT_MAX_LINES = 2

@Composable
fun <T> AppSegmented(
    modifier: Modifier = Modifier,
    options: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TRACK_HEIGHT)
            .background(
                color = AppTheme.colors.bgSurfaceSunken,
                shape = RoundedCornerShape(AppTheme.radius.dp8),
            )
            .padding(TRACK_PADDING),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(option) }
                    .background(
                        color = if (isSelected) AppTheme.colors.bgSurface else AppTheme.colors.bgSurfaceSunken,
                        shape = RoundedCornerShape(SEGMENT_RADIUS),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = labelOf(option),
                    style = if (isSelected) AppTheme.typography.bodyStrong else AppTheme.typography.body,
                    color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                    maxLines = SEGMENT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
