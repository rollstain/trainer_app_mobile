package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val SEATS_CHIP_HEIGHT = 28.dp
private val SEATS_CHIP_PADDING = 10.dp

enum class SeatsState {
    Empty,
    Free,
    Full,
    ;

    companion object {

        fun of(taken: Int, capacity: Int): SeatsState = when {
            taken <= 0 -> Empty
            taken >= capacity -> Full
            else -> Free
        }
    }
}

@Composable
fun AppSeatsChip(modifier: Modifier = Modifier, label: String, state: SeatsState) {
    val colors = AppTheme.colors
    val background: Color
    val content: Color
    when (state) {
        SeatsState.Empty -> {
            background = colors.bgSurfaceSunken
            content = colors.textSecondary
        }
        SeatsState.Free -> {
            background = colors.successSoft
            content = colors.success
        }
        SeatsState.Full -> {
            background = colors.warningSoft
            content = colors.warning
        }
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = SEATS_CHIP_HEIGHT)
            .background(color = background, shape = RoundedCornerShape(AppTheme.radius.pill))
            .padding(horizontal = SEATS_CHIP_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = label,
            style = AppTheme.typography.numeric,
            color = content,
            maxLines = 1,
        )
    }
}
