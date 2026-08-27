package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val CELL_GAP = 4.dp
private val STRIP_PADDING = 8.dp
private val LABEL_TO_NUMBER_GAP = 3.dp
private val LOAD_DOT_SIZE = 4.dp

enum class WeekDayState { Rest, Selected, Today, Weekend }

data class WeekDay(
    val id: String,
    val weekdayLabel: String,
    val dayNumber: String,
    val state: WeekDayState,
    val hasSlots: Boolean,
)

@Composable
fun AppWeekStrip(
    modifier: Modifier = Modifier,
    days: List<WeekDay>,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.bgScreen)
            .padding(STRIP_PADDING),
        horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        days.forEach { day ->
            WeekDayCell(
                modifier = Modifier.weight(1f),
                day = day,
                onClick = { onSelect(day.id) },
            )
        }
    }
}

@Composable
private fun WeekDayCell(modifier: Modifier, day: WeekDay, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val isSelected = day.state == WeekDayState.Selected
    val background = when (day.state) {
        WeekDayState.Selected -> colors.accent
        WeekDayState.Weekend -> colors.bgScreen
        WeekDayState.Rest, WeekDayState.Today -> colors.bgSurfaceSunken
    }
    val contentColor = when (day.state) {
        WeekDayState.Selected -> colors.accentOn
        WeekDayState.Weekend -> colors.textMuted
        WeekDayState.Rest, WeekDayState.Today -> colors.textPrimary
    }
    val shape = RoundedCornerShape(AppTheme.radius.dp8)

    Column(
        modifier = modifier
            .heightIn(min = AppTheme.sizing.weekDayCell)
            .background(color = background, shape = shape)
            .then(
                if (day.state == WeekDayState.Today) {
                    Modifier.border(
                        width = AppTheme.borders.field,
                        color = colors.borderStrong,
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = day.weekdayLabel,
            style = AppTheme.typography.overline,
            color = if (isSelected) colors.accentOn else colors.textSecondary,
        )
        Box(modifier = Modifier.height(LABEL_TO_NUMBER_GAP))
        Text(
            text = day.dayNumber,
            style = AppTheme.typography.numeric.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
        )
        LoadDot(hasSlots = day.hasSlots, color = if (isSelected) colors.accentOn else colors.accent)
    }
}

@Composable
private fun LoadDot(hasSlots: Boolean, color: Color) {
    if (!hasSlots) return
    Box(
        modifier = Modifier
            .size(LOAD_DOT_SIZE)
            .background(color = color, shape = CircleShape),
    )
}
