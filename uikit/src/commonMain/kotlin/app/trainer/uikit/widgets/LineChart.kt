package app.trainer.uikit.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import kotlinx.collections.immutable.ImmutableList

private val CHART_HEIGHT = 132.dp
private val CHART_LINE_WIDTH = 2.dp
private val CHART_DOT_RADIUS = 3.dp
private val BASELINE_DASH_ON = 3.dp
private val BASELINE_DASH_OFF = 3.dp
private const val SINGLE_POINT_HORIZONTAL_CENTER = 0.5f
private const val FLAT_SERIES_VERTICAL_CENTER = 0.5f

private class ChartMetrics(
    val lineWidth: Float,
    val dotRadius: Float,
    val dashOn: Float,
    val dashOff: Float,
)

@Composable
fun AppLineChart(
    modifier: Modifier = Modifier,
    values: ImmutableList<Float>,
    maxLabel: String,
    minLabel: String,
    rangeLabel: String,
) {
    val lineColor = AppTheme.colors.accent
    val baselineColor = AppTheme.colors.border
    val metrics = with(LocalDensity.current) {
        ChartMetrics(
            lineWidth = CHART_LINE_WIDTH.toPx(),
            dotRadius = CHART_DOT_RADIUS.toPx(),
            dashOn = BASELINE_DASH_ON.toPx(),
            dashOff = BASELINE_DASH_OFF.toPx(),
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
        AxisLabel(text = maxLabel)
        Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            drawBaseline(color = baselineColor, metrics = metrics)
            drawSeries(
                offsets = offsetsOf(values = values, width = size.width, height = size.height),
                color = lineColor,
                metrics = metrics,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AxisLabel(text = minLabel)
            AxisLabel(text = rangeLabel)
        }
    }
}

@Composable
private fun AxisLabel(text: String) {
    AppText(
        text = text,
        style = AppTheme.typography.overline,
        color = AppTheme.colors.textMuted,
    )
}

private fun offsetsOf(values: List<Float>, width: Float, height: Float): List<Offset> {
    if (values.isEmpty()) return emptyList()
    if (values.size == 1) {
        return listOf(
            Offset(
                x = width * SINGLE_POINT_HORIZONTAL_CENTER,
                y = height * FLAT_SERIES_VERTICAL_CENTER,
            )
        )
    }
    val minValue = values.min()
    val span = values.max() - minValue
    val horizontalStep = width / (values.size - 1)
    return values.mapIndexed { index, value ->
        val verticalFraction = if (span == 0f) {
            FLAT_SERIES_VERTICAL_CENTER
        } else {
            (value - minValue) / span
        }
        Offset(x = horizontalStep * index, y = height - height * verticalFraction)
    }
}

private fun DrawScope.drawBaseline(color: Color, metrics: ChartMetrics) {
    drawLine(
        color = color,
        start = Offset(x = 0f, y = size.height),
        end = Offset(x = size.width, y = size.height),
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(metrics.dashOn, metrics.dashOff),
        ),
    )
}

private fun DrawScope.drawSeries(offsets: List<Offset>, color: Color, metrics: ChartMetrics) {
    if (offsets.size > 1) {
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { offset -> lineTo(offset.x, offset.y) }
        }
        drawPath(path = path, color = color, style = Stroke(width = metrics.lineWidth))
    }
    offsets.forEach { offset ->
        drawCircle(color = color, radius = metrics.dotRadius, center = offset)
    }
}
