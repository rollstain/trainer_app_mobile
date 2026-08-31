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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
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
private val LINE_WIDTH_FRACTIONS = listOf(0.5f, 0.85f, 0.65f)
private const val SLOT_TITLE_WIDTH_FRACTION = 0.55f
private const val SLOT_SUBTITLE_WIDTH_FRACTION = 0.35f
private val SETTING_LABEL_WIDTH = 44.dp
private const val OWN_BUBBLE_WIDTH_FRACTION = 0.62f
private const val OTHER_BUBBLE_WIDTH_FRACTION = 0.74f
private val BUBBLE_HEIGHTS = listOf(44.dp, 68.dp, 40.dp, 88.dp, 52.dp)

@Composable
private fun rememberShimmerAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = SHIMMER_MIN_ALPHA,
        targetValue = SHIMMER_MAX_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    return alpha
}

@Composable
private fun Modifier.shimmerBar(): Modifier = background(
    color = AppTheme.colors.bgSurfaceSunken,
    shape = RoundedCornerShape(AppTheme.radius.dp4),
)

@Composable
private fun ShimmerCell(rowHeight: Dp, isLastRow: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .alpha(if (isLastRow) LAST_ROW_ALPHA else 1f)
            .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(AppTheme.sizing.avatarLarge)
                .background(color = AppTheme.colors.bgSurfaceSunken, shape = CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(TITLE_WIDTH_FRACTION)
                    .height(TITLE_BAR_HEIGHT)
                    .shimmerBar(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(PREVIEW_WIDTH_FRACTION)
                    .height(PREVIEW_BAR_HEIGHT)
                    .shimmerBar(),
            )
        }
    }
}

@Composable
private fun ShimmerCard(lines: Int) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            repeat(lines) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(LINE_WIDTH_FRACTIONS[index % LINE_WIDTH_FRACTIONS.size])
                        .height(if (index == 0) TITLE_BAR_HEIGHT else PREVIEW_BAR_HEIGHT)
                        .shimmerBar(),
                )
            }
        }
    }
}

@Composable
private fun ShimmerSlot() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.sizing.slotCardMinHeight)
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(horizontal = AppTheme.spacing.dp12, vertical = AppTheme.spacing.dp12),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(AppTheme.sizing.slotTimeColumnWidth)
                .height(TITLE_BAR_HEIGHT)
                .shimmerBar(),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(SLOT_TITLE_WIDTH_FRACTION)
                    .height(TITLE_BAR_HEIGHT)
                    .shimmerBar(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(SLOT_SUBTITLE_WIDTH_FRACTION)
                    .height(PREVIEW_BAR_HEIGHT)
                    .shimmerBar(),
            )
        }
    }
}

@Composable
private fun ShimmerBubble(index: Int) {
    val isOwn = index % 2 == 0
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isOwn) OWN_BUBBLE_WIDTH_FRACTION else OTHER_BUBBLE_WIDTH_FRACTION)
                .height(BUBBLE_HEIGHTS[index % BUBBLE_HEIGHTS.size])
                .background(
                    color = if (isOwn) AppTheme.colors.accentSoft else AppTheme.colors.bgSurfaceSunken,
                    shape = RoundedCornerShape(AppTheme.radius.dp12),
                ),
        )
    }
}

@Composable
fun AppCardShimmer(modifier: Modifier = Modifier, lines: Int) {
    Box(modifier = modifier.alpha(rememberShimmerAlpha())) {
        ShimmerCard(lines = lines)
    }
}

@Composable
fun AppCardShimmerList(modifier: Modifier = Modifier, count: Int, lines: Int) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(rememberShimmerAlpha())
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        repeat(count) { ShimmerCard(lines = lines) }
    }
}

@Composable
fun AppSlotShimmerList(modifier: Modifier = Modifier, count: Int) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(rememberShimmerAlpha())
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        repeat(count) { ShimmerSlot() }
    }
}

@Composable
fun AppCellShimmerList(
    modifier: Modifier = Modifier,
    count: Int,
    rowHeight: Dp = AppTheme.sizing.cellLarge,
) {
    Column(modifier = modifier.fillMaxSize().alpha(rememberShimmerAlpha())) {
        repeat(count) { index ->
            ShimmerCell(rowHeight = rowHeight, isLastRow = index == count - 1)
        }
    }
}

@Composable
private fun ShimmerSettingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.sizing.cellSmall)
            .padding(horizontal = AppTheme.spacing.dp16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(SETTING_LABEL_WIDTH)
                .height(TITLE_BAR_HEIGHT)
                .shimmerBar(),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(PREVIEW_BAR_HEIGHT)
                .shimmerBar(),
        )
    }
}

@Composable
fun AppSettingShimmerList(modifier: Modifier = Modifier, count: Int) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(rememberShimmerAlpha())
            .background(AppTheme.colors.bgSurface),
    ) {
        repeat(count) { ShimmerSettingRow() }
    }
}

@Composable
fun AppMessageShimmerList(modifier: Modifier = Modifier, count: Int) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .alpha(rememberShimmerAlpha())
            .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        repeat(count) { index -> ShimmerBubble(index = index) }
    }
}
