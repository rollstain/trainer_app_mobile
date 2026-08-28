package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val CHIP_HEIGHT = 36.dp
private val CHIP_PADDING_HORIZONTAL = 14.dp
private val CHIP_TOUCH_PADDING_VERTICAL = 4.dp
private val REMOVABLE_CHIP_HEIGHT = 28.dp
private val REMOVABLE_CHIP_PADDING = 10.dp
private const val PILL_RADIUS = 999

@Composable
fun AppChoiceChip(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(PILL_RADIUS.dp)
    Box(
        modifier = modifier
            .padding(vertical = CHIP_TOUCH_PADDING_VERTICAL)
            .defaultMinSize(minHeight = CHIP_HEIGHT)
            .background(color = if (isSelected) colors.accent else colors.bgSurface, shape = shape)
            .then(
                if (isSelected) {
                    Modifier
                } else {
                    Modifier.border(width = AppTheme.borders.hairline, color = colors.borderStrong, shape = shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = CHIP_PADDING_HORIZONTAL),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = text,
            style = if (isSelected) AppTheme.typography.bodyStrong else AppTheme.typography.body,
            color = if (isSelected) colors.accentOn else colors.textPrimary,
        )
    }
}

@Composable
fun <T> AppChipRow(
    modifier: Modifier = Modifier,
    options: List<T>,
    isSelected: (T) -> Boolean,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
        options.forEach { option ->
            AppChoiceChip(
                text = labelOf(option),
                isSelected = isSelected(option),
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
fun AppRemovableChip(
    modifier: Modifier = Modifier,
    text: String,
    removeDescription: String,
    onRemove: () -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(PILL_RADIUS.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = REMOVABLE_CHIP_HEIGHT)
            .background(color = colors.accentSoft, shape = shape)
            .clickable(onClick = onRemove)
            .padding(horizontal = REMOVABLE_CHIP_PADDING),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = text,
            style = AppTheme.typography.caption,
            color = colors.textPrimary,
        )
        AppIcon(
            painter = AppIcons.close,
            contentDescription = removeDescription,
            size = IconSize.Small,
            tint = colors.textSecondary,
        )
    }
}
