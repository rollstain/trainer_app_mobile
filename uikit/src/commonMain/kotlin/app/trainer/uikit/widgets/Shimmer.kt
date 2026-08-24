package app.trainer.uikit.widgets

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private const val SHIMMER_DURATION_MS = 1_200
private const val SHIMMER_MIN_ALPHA = 0.45f
private const val SHIMMER_MAX_ALPHA = 1f
private const val LAST_ROW_ALPHA = 0.6f
private const val TITLE_WIDTH_FRACTION = 0.5f
private const val PREVIEW_WIDTH_FRACTION = 0.7f
private val TITLE_BAR_HEIGHT = 14.dp
private val PREVIEW_BAR_HEIGHT = 12.dp

@Composable
fun Modifier.shimmerable(isLoading: Boolean): Modifier {
    if (!isLoading) return this
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = SHIMMER_MIN_ALPHA,
        targetValue = SHIMMER_MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    return this
        .alpha(shimmerAlpha)
        .background(
            color = AppTheme.colors.bgSurfaceSunken,
            shape = RoundedCornerShape(AppTheme.radius.dp4),
        )
}

@Composable
fun AppCellShimmer(modifier: Modifier = Modifier, isLastRow: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.sizing.cellLarge)
            .alpha(if (isLastRow) LAST_ROW_ALPHA else 1f)
            .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AppTheme.sizing.avatarLarge)
                .background(color = AppTheme.colors.bgSurfaceSunken, shape = CircleShape)
                .shimmerable(isLoading = true),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(TITLE_WIDTH_FRACTION)
                    .height(TITLE_BAR_HEIGHT)
                    .shimmerable(isLoading = true),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(PREVIEW_WIDTH_FRACTION)
                    .height(PREVIEW_BAR_HEIGHT)
                    .shimmerable(isLoading = true),
            )
        }
    }
}
