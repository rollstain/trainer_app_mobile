package app.trainer.uikit.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.resources.Res
import app.trainer.uikit.resources.rest_extend
import app.trainer.uikit.resources.rest_skip
import app.trainer.uikit.resources.rest_title
import org.jetbrains.compose.resources.stringResource

private val PROGRESS_HEIGHT = 3.dp

@Composable
fun AppRestBar(
    modifier: Modifier = Modifier,
    label: String,
    progress: Float,
    onExtend: () -> Unit,
    onSkip: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = AppTheme.motion.stateChangeMillis,
            easing = AppTheme.motion.easeOut,
        ),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.accentSoft,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(AppTheme.spacing.dp12),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.rest_title),
                style = AppTheme.typography.label,
                color = AppTheme.colors.accent,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = AppTheme.typography.numeric,
                color = AppTheme.colors.accent,
            )
            AppButton(
                text = stringResource(Res.string.rest_extend),
                onClick = onExtend,
                tone = ButtonTone.Text,
                size = ButtonSize.Small,
            )
            AppButton(
                text = stringResource(Res.string.rest_skip),
                onClick = onSkip,
                tone = ButtonTone.Text,
                size = ButtonSize.Small,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PROGRESS_HEIGHT)
                .background(
                    color = AppTheme.colors.bgSurface,
                    shape = RoundedCornerShape(AppTheme.radius.pill),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        color = AppTheme.colors.accent,
                        shape = RoundedCornerShape(AppTheme.radius.pill),
                    ),
            )
        }
    }
}
