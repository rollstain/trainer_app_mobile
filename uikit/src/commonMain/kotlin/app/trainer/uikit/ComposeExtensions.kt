package app.trainer.uikit

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DASH_ON = 4.dp
private val DASH_OFF = 3.dp

@Composable
fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier {
    val strokeWidth = AppTheme.borders.hairline
    return drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(DASH_ON.toPx(), DASH_OFF.toPx()),
                ),
            ),
        )
    }
}

@Composable
fun Modifier.leadingStripe(color: Color, width: Dp): Modifier {
    return drawBehind {
        drawRect(
            color = color,
            size = androidx.compose.ui.geometry.Size(width = width.toPx(), height = size.height),
        )
    }
}

@Composable
fun Modifier.screenBackground(): Modifier = background(AppTheme.colors.bgScreen)
