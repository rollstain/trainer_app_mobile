package app.trainer.uikit.semantic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

@Immutable
data class AppMotion(
    val stateChangeMillis: Int = 150,
    val screenTransitionMillis: Int = 200,
    val shimmerCycleMillis: Int = 1_200,
    val easeOut: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f),
)
