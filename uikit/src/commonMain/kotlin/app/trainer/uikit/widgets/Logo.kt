package app.trainer.uikit.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val LOGO_DEFAULT_SIZE = 56.dp
private val LOGO_CORNER = 14.dp
private const val LOGO_VIEWPORT = 108f
private const val LOGO_SAFE_ZONE = 72f

private class LogoBar(val left: Float, val top: Float, val right: Float, val bottom: Float)

private val LOGO_BARS = listOf(
    LogoBar(left = 26f, top = 48f, right = 32f, bottom = 60f),
    LogoBar(left = 34f, top = 42f, right = 42f, bottom = 66f),
    LogoBar(left = 42f, top = 51f, right = 66f, bottom = 57f),
    LogoBar(left = 66f, top = 42f, right = 74f, bottom = 66f),
    LogoBar(left = 76f, top = 48f, right = 82f, bottom = 60f),
)

@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = LOGO_DEFAULT_SIZE) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(color = colors.accent, shape = RoundedCornerShape(LOGO_CORNER)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = this.size.minDimension / LOGO_SAFE_ZONE
            val inset = (LOGO_VIEWPORT - LOGO_SAFE_ZONE) / 2
            LOGO_BARS.forEach { bar ->
                drawRect(
                    color = colors.accentOn,
                    topLeft = Offset(x = (bar.left - inset) * scale, y = (bar.top - inset) * scale),
                    size = Size(
                        width = (bar.right - bar.left) * scale,
                        height = (bar.bottom - bar.top) * scale,
                    ),
                )
            }
        }
    }
}
