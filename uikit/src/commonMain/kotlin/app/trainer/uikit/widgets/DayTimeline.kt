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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder
import app.trainer.uikit.leadingStripe

private const val SCALE_START_HOUR = 6
private const val SCALE_END_HOUR = 23
private const val HOUR_LABEL_STEP = 3
private const val MINUTES_IN_HOUR = 60
private const val MAX_PARALLEL_COLUMNS = 2
private val SLOT_HEIGHT = 38.dp
private val SLOT_LEFT_INSET = 8.dp
private val SLOT_PADDING = 12.dp
private val SLOT_COLUMN_GAP = 4.dp

data class TimelineSlot(
    val id: String,
    val startMinutes: Int,
    val durationMinutes: Int,
    val timeLabel: String,
    val title: String,
    val durationLabel: String,
    val status: SlotStatusView,
    val hasRequest: Boolean,
)

@Composable
fun AppDayTimeline(
    modifier: Modifier = Modifier,
    slots: List<TimelineSlot>,
    onSlotClick: (String) -> Unit,
) {
    val hourHeight = AppTheme.sizing.timelineHourHeight
    val totalHeight = hourHeight * (SCALE_END_HOUR - SCALE_START_HOUR + 1)
    val columnsBySlot = assignColumns(slots)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .height(totalHeight),
    ) {
        HourColumn(hourHeight = hourHeight)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(totalHeight)
                .leadingStripe(color = AppTheme.colors.border, width = AppTheme.borders.hairline),
        ) {
            slots.forEach { slot ->
                val column = columnsBySlot.getValue(slot.id)
                TimelineSlotCard(
                    modifier = Modifier
                        .offset(y = offsetOf(slot = slot, hourHeight = hourHeight))
                        .padding(start = SLOT_LEFT_INSET),
                    slot = slot,
                    column = column,
                    onClick = { onSlotClick(slot.id) },
                )
            }
        }
    }
}

@Composable
private fun HourColumn(hourHeight: Dp) {
    Column(modifier = Modifier.width(AppTheme.sizing.timelineHourColumnWidth)) {
        (SCALE_START_HOUR..SCALE_END_HOUR).forEach { hour ->
            Box(modifier = Modifier.height(hourHeight)) {
                if ((hour - SCALE_START_HOUR) % HOUR_LABEL_STEP == 0) {
                    Text(
                        text = hour.toString().padStart(length = 2, padChar = '0'),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineSlotCard(
    modifier: Modifier,
    slot: TimelineSlot,
    column: Int,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.dp8)
    val background = when {
        slot.hasRequest -> colors.warningSoft
        slot.status == SlotStatusView.Completed -> colors.bgSurfaceSunken
        slot.status == SlotStatusView.Booked -> colors.accentSoft
        slot.status == SlotStatusView.Cancelled -> colors.bgScreen
        else -> colors.bgSurface
    }
    val widthFraction = if (column == 0) 1f else 1f / MAX_PARALLEL_COLUMNS

    Row(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(SLOT_HEIGHT)
            .padding(start = if (column > 0) SLOT_COLUMN_GAP else 0.dp)
            .background(color = background, shape = shape)
            .then(
                when {
                    slot.hasRequest -> Modifier.dashedBorder(
                        color = colors.warning,
                        cornerRadius = AppTheme.radius.dp8,
                    )
                    slot.status == SlotStatusView.Booked -> Modifier.leadingStripe(
                        color = colors.accent,
                        width = AppTheme.borders.accentStripe,
                    )
                    slot.status == SlotStatusView.Free -> Modifier.border(
                        width = AppTheme.borders.hairline,
                        color = colors.border,
                        shape = shape,
                    )
                    else -> Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = SLOT_PADDING),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = slot.timeLabel,
            style = AppTheme.typography.label,
            color = colors.textPrimary,
            textDecoration = if (slot.status == SlotStatusView.Cancelled) {
                TextDecoration.LineThrough
            } else {
                null
            },
        )
        Text(
            modifier = Modifier.weight(1f),
            text = slot.title,
            style = AppTheme.typography.label,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = slot.durationLabel,
            style = AppTheme.typography.caption,
            color = colors.textMuted,
        )
    }
}

private fun offsetOf(slot: TimelineSlot, hourHeight: Dp): Dp {
    val minutesFromScaleStart = slot.startMinutes - SCALE_START_HOUR * MINUTES_IN_HOUR
    return hourHeight * (minutesFromScaleStart.toFloat() / MINUTES_IN_HOUR)
}

private fun assignColumns(slots: List<TimelineSlot>): Map<String, Int> {
    val ordered = slots.sortedBy { it.startMinutes }
    val columns = mutableMapOf<String, Int>()
    val occupiedUntil = IntArray(MAX_PARALLEL_COLUMNS)

    ordered.forEach { slot ->
        val endMinutes = slot.startMinutes + slot.durationMinutes
        val freeColumn = (0 until MAX_PARALLEL_COLUMNS).firstOrNull { column ->
            occupiedUntil[column] <= slot.startMinutes
        } ?: 0
        occupiedUntil[freeColumn] = endMinutes
        columns[slot.id] = freeColumn
    }
    return columns
}
