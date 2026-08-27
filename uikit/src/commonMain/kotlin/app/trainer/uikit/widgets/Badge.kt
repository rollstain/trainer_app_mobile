package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import app.trainer.uikit.AppTheme

private const val MAX_DISPLAYED_COUNT = 99
private const val OVERFLOW_SUFFIX = "99+"

enum class BadgeTone { Regular, Critical }

sealed interface BadgeValue {

    data object Dot : BadgeValue

    data class Count(val value: Long) : BadgeValue
}

@Composable
fun AppBadge(
    modifier: Modifier = Modifier,
    value: BadgeValue,
    tone: BadgeTone = BadgeTone.Regular,
) {
    val background = when (tone) {
        BadgeTone.Regular -> AppTheme.colors.accent
        BadgeTone.Critical -> AppTheme.colors.danger
    }
    when (value) {
        BadgeValue.Dot -> Box(
            modifier = modifier
                .size(AppTheme.sizing.badgeDot)
                .background(color = background, shape = CircleShape),
        )
        is BadgeValue.Count -> Box(
            modifier = modifier
                .heightIn(min = AppTheme.sizing.badgeHeight)
                .defaultMinSize(minWidth = AppTheme.sizing.badgeMinWidth)
                .background(
                    color = background,
                    shape = RoundedCornerShape(AppTheme.radius.pill),
                )
                .padding(horizontal = AppTheme.sizing.badgePadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatCount(value.value),
                style = AppTheme.typography.overline.copy(fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.accentOn,
            )
        }
    }
}

private fun formatCount(count: Long): String {
    return if (count > MAX_DISPLAYED_COUNT) OVERFLOW_SUFFIX else count.toString()
}
