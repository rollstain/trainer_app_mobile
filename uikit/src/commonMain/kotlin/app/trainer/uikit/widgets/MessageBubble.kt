package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder
import app.trainer.uikit.resources.Res
import app.trainer.uikit.resources.message_delivery_failed
import app.trainer.uikit.resources.message_delivery_pending
import app.trainer.uikit.resources.message_delivery_read
import app.trainer.uikit.resources.message_delivery_sent
import app.trainer.uikit.resources.message_photo_description
import org.jetbrains.compose.resources.stringResource

private const val PENDING_ALPHA = 0.55f
private const val MAX_WIDTH_FRACTION = 0.78f
private val BUBBLE_PADDING_VERTICAL = 10.dp
private val BUBBLE_PADDING_HORIZONTAL = 12.dp
private val TAIL_RADIUS = 4.dp
private val BUBBLE_PHOTO_HEIGHT = 200.dp

data class BubbleAttachment(
    val id: String,
    val url: String?,
)

enum class MessageSide { Own, Other }

enum class MessageDeliveryState { Pending, Sent, Read, Failed }

@Composable
fun AppMessageBubble(
    modifier: Modifier = Modifier,
    text: String,
    time: String,
    side: MessageSide,
    delivery: MessageDeliveryState,
    onRetryClick: () -> Unit,
    onAttachmentClick: (String) -> Unit,
    attachments: List<BubbleAttachment> = emptyList(),
) {
    val colors = AppTheme.colors
    val isOwn = side == MessageSide.Own
    val background = when {
        delivery == MessageDeliveryState.Failed -> colors.dangerSoft
        isOwn -> colors.accentSoft
        else -> colors.bgSurface
    }
    val shape = if (isOwn) {
        RoundedCornerShape(
            topStart = AppTheme.radius.dp12,
            topEnd = AppTheme.radius.dp12,
            bottomEnd = TAIL_RADIUS,
            bottomStart = AppTheme.radius.dp12,
        )
    } else {
        RoundedCornerShape(
            topStart = AppTheme.radius.dp12,
            topEnd = AppTheme.radius.dp12,
            bottomEnd = AppTheme.radius.dp12,
            bottomStart = TAIL_RADIUS,
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (delivery == MessageDeliveryState.Pending || delivery == MessageDeliveryState.Failed) {
            DeliveryMarker(delivery = delivery, onClick = onRetryClick)
            Box(modifier = Modifier.size(AppTheme.spacing.dp8))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(MAX_WIDTH_FRACTION)
                .alpha(if (delivery == MessageDeliveryState.Pending) PENDING_ALPHA else 1f)
                .background(color = background, shape = shape)
                .then(bubbleBorder(side = side, delivery = delivery, shape = shape))
                .padding(
                    horizontal = BUBBLE_PADDING_HORIZONTAL,
                    vertical = BUBBLE_PADDING_VERTICAL,
                ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
        ) {
            attachments.forEach { attachment ->
                BubblePhoto(
                    attachment = attachment,
                    onClick = { onAttachmentClick(attachment.id) },
                )
            }
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    style = AppTheme.typography.body,
                    color = colors.textPrimary,
                )
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = time,
                    style = AppTheme.typography.overline,
                    color = colors.textMuted,
                )
                if (isOwn) {
                    OwnDeliveryIcon(delivery = delivery)
                }
            }
        }
    }
}

@Composable
private fun BubblePhoto(attachment: BubbleAttachment, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BUBBLE_PHOTO_HEIGHT)
            .clip(RoundedCornerShape(AppTheme.radius.dp8))
            .background(AppTheme.colors.bgSurfaceSunken)
            .clickable(onClick = onClick),
    ) {
        if (attachment.url != null) {
            AppRemoteImage(
                modifier = Modifier.fillMaxSize(),
                url = attachment.url,
                cacheKey = attachment.id,
                contentDescription = stringResource(Res.string.message_photo_description),
            )
        }
    }
}

@Composable
private fun OwnDeliveryIcon(delivery: MessageDeliveryState) {
    when (delivery) {
        MessageDeliveryState.Pending, MessageDeliveryState.Failed -> Unit
        MessageDeliveryState.Sent -> AppIcon(
            painter = AppIcons.sent,
            contentDescription = stringResource(Res.string.message_delivery_sent),
            size = IconSize.Small,
            tint = AppTheme.colors.accent,
        )
        MessageDeliveryState.Read -> AppIcon(
            painter = AppIcons.read,
            contentDescription = stringResource(Res.string.message_delivery_read),
            size = IconSize.Small,
            tint = AppTheme.colors.accent,
        )
    }
}

@Composable
private fun DeliveryMarker(delivery: MessageDeliveryState, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val size = AppTheme.sizing.deliveryMarker
    when (delivery) {
        MessageDeliveryState.Sent, MessageDeliveryState.Read -> Unit
        MessageDeliveryState.Pending -> Box(
            modifier = Modifier
                .size(size)
                .dashedBorder(color = colors.warning, cornerRadius = size / 2),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                painter = AppIcons.pending,
                contentDescription = stringResource(Res.string.message_delivery_pending),
                size = IconSize.Small,
                tint = colors.warning,
            )
        }
        MessageDeliveryState.Failed -> Box(
            modifier = Modifier
                .size(size)
                .background(color = colors.danger, shape = CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                painter = AppIcons.failed,
                contentDescription = stringResource(Res.string.message_delivery_failed),
                size = IconSize.Small,
                tint = colors.accentOn,
            )
        }
    }
}

@Composable
private fun bubbleBorder(
    side: MessageSide,
    delivery: MessageDeliveryState,
    shape: RoundedCornerShape,
): Modifier = when {
    delivery == MessageDeliveryState.Failed -> Modifier.border(
        width = AppTheme.borders.hairline,
        color = AppTheme.colors.danger,
        shape = shape,
    )
    side == MessageSide.Other -> Modifier.border(
        width = AppTheme.borders.hairline,
        color = AppTheme.colors.border,
        shape = shape,
    )
    else -> Modifier
}
