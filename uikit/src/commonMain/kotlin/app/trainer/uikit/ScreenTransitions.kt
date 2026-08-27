package app.trainer.uikit

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

private const val OUTGOING_SCREEN_SHIFT_FRACTION = 0.25f

@Composable
fun forwardScreenTransition(): ContentTransform = screenTransition(
    incomingOffset = { fullWidth -> fullWidth },
    outgoingOffset = { fullWidth -> -partialShiftOf(fullWidth) },
)

@Composable
fun backwardScreenTransition(): ContentTransform = screenTransition(
    incomingOffset = { fullWidth -> -partialShiftOf(fullWidth) },
    outgoingOffset = { fullWidth -> fullWidth },
)

@Composable
private fun screenTransition(
    incomingOffset: (Int) -> Int,
    outgoingOffset: (Int) -> Int,
): ContentTransform {
    val durationMillis = AppTheme.motion.screenTransitionMillis
    val easing = AppTheme.motion.easeOut
    val fadeSpec = tween<Float>(durationMillis = durationMillis, easing = easing)
    val slideSpec = tween<IntOffset>(durationMillis = durationMillis, easing = easing)
    return slideInHorizontally(animationSpec = slideSpec, initialOffsetX = incomingOffset) +
        fadeIn(animationSpec = fadeSpec) togetherWith
        slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = outgoingOffset) +
        fadeOut(animationSpec = fadeSpec)
}

private fun partialShiftOf(fullWidth: Int): Int =
    (fullWidth * OUTGOING_SCREEN_SHIFT_FRACTION).toInt()
