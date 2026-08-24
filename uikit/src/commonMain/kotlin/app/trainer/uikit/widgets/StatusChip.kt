package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder

private val CONTENT_GAP = 6.dp

enum class StatusChipKind { Free, Booked, Cancelled, Completed, PendingRequest, Medical }

@Composable
fun AppStatusChip(
    modifier: Modifier = Modifier,
    text: String,
    kind: StatusChipKind,
) {
    val colors = AppTheme.colors
    val background = when (kind) {
        StatusChipKind.Free -> colors.successSoft
        StatusChipKind.Booked -> colors.accentSoft
        StatusChipKind.Cancelled, StatusChipKind.Completed -> colors.bgSurfaceSunken
        StatusChipKind.PendingRequest -> colors.warningSoft
        StatusChipKind.Medical -> colors.dangerSoft
    }
    val content = when (kind) {
        StatusChipKind.Free -> colors.success
        StatusChipKind.Booked -> colors.accent
        StatusChipKind.Cancelled, StatusChipKind.Completed -> colors.textSecondary
        StatusChipKind.PendingRequest -> colors.warning
        StatusChipKind.Medical -> colors.danger
    }
    val hasDot = kind == StatusChipKind.Free || kind == StatusChipKind.Booked
    val chipShape = RoundedCornerShape(AppTheme.radius.pill)
    val chipModifier = modifier
        .height(AppTheme.sizing.chipHeight)
        .background(color = background, shape = chipShape)
        .let { base ->
            if (kind == StatusChipKind.PendingRequest) {
                base.dashedBorder(color = content, cornerRadius = AppTheme.sizing.chipHeight / 2)
            } else {
                base
            }
        }
        .padding(horizontal = AppTheme.sizing.chipPadding)

    Row(
        modifier = chipModifier,
        horizontalArrangement = Arrangement.spacedBy(CONTENT_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasDot) {
            Box(
                modifier = Modifier
                    .size(AppTheme.sizing.chipDot)
                    .background(color = content, shape = CircleShape),
            )
        }
        Text(
            text = text,
            style = AppTheme.typography.caption,
            color = content,
            textDecoration = if (kind == StatusChipKind.Cancelled) TextDecoration.LineThrough else null,
        )
    }
}
