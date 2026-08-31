package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val WEEKDAY_CELL_SIZE = 44.dp
private val WEEKDAY_CELL_GAP = 6.dp

@Composable
fun <T> AppWeekdayPicker(
    modifier: Modifier = Modifier,
    days: List<T>,
    labelOf: (T) -> String,
    isSelected: (T) -> Boolean,
    onToggle: (T) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WEEKDAY_CELL_GAP),
    ) {
        days.forEach { day ->
            val selected = isSelected(day)
            val shape = RoundedCornerShape(AppTheme.radius.dp8)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(WEEKDAY_CELL_SIZE)
                    .defaultMinSize(minWidth = WEEKDAY_CELL_SIZE)
                    .clickable { onToggle(day) }
                    .background(
                        color = if (selected) AppTheme.colors.accent else AppTheme.colors.bgSurface,
                        shape = shape,
                    )
                    .then(
                        if (selected) {
                            Modifier
                        } else {
                            Modifier.border(
                                width = AppTheme.borders.hairline,
                                color = AppTheme.colors.borderStrong,
                                shape = shape,
                            )
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = labelOf(day),
                    style = if (selected) AppTheme.typography.bodyStrong else AppTheme.typography.body,
                    color = if (selected) AppTheme.colors.accentOn else AppTheme.colors.textPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}
