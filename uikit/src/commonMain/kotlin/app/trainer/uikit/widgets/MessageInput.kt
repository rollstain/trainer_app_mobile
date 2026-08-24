package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val ACTION_BUTTON_SIZE = 40.dp
private val INPUT_MIN_HEIGHT = 40.dp
private val INPUT_MAX_HEIGHT = 120.dp
private val INPUT_PADDING_HORIZONTAL = 12.dp
private val INPUT_PADDING_VERTICAL = 10.dp
private val BAR_PADDING = 8.dp
private val PROGRESS_SIZE = 24.dp
private const val UPLOADING_SCRIM_ALPHA = 0.4f

enum class AttachmentState { Ready, Uploading, Failed }

data class AttachmentPreview(
    val id: String,
    val fileName: String,
    val sizeLabel: String,
    val isImage: Boolean,
    val state: AttachmentState,
)

@Composable
fun AppMessageInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    attachments: List<AttachmentPreview>,
    onAttachClick: () -> Unit,
    onSendClick: () -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onAttachmentRetry: (String) -> Unit,
    placeholder: String = "Сообщение",
) {
    val colors = AppTheme.colors
    val isSendEnabled = value.isNotBlank() || attachments.any { it.state == AttachmentState.Ready }

    Column(modifier = modifier.fillMaxWidth().background(colors.bgSurface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.borders.hairline)
                .background(colors.border),
        )
        if (attachments.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BAR_PADDING, vertical = BAR_PADDING),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                items(items = attachments, key = { it.id }) { attachment ->
                    AttachmentChip(
                        attachment = attachment,
                        onRemove = { onAttachmentRemove(attachment.id) },
                        onRetry = { onAttachmentRetry(attachment.id) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(BAR_PADDING),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .size(ACTION_BUTTON_SIZE)
                    .background(
                        color = colors.bgSurfaceSunken,
                        shape = RoundedCornerShape(AppTheme.radius.dp8),
                    )
                    .clickable(onClick = onAttachClick),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    painter = AppIcons.attach,
                    contentDescription = "Прикрепить файл",
                    size = IconSize.Medium,
                    tint = colors.textSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = INPUT_MIN_HEIGHT, max = INPUT_MAX_HEIGHT)
                    .background(
                        color = colors.bgSurfaceSunken,
                        shape = RoundedCornerShape(AppTheme.radius.dp8),
                    )
                    .padding(
                        horizontal = INPUT_PADDING_HORIZONTAL,
                        vertical = INPUT_PADDING_VERTICAL,
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = AppTheme.typography.body,
                        color = colors.textMuted,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = AppTheme.typography.body.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                )
            }
            Box(
                modifier = Modifier
                    .size(ACTION_BUTTON_SIZE)
                    .background(
                        color = if (isSendEnabled) colors.accent else colors.bgSurfaceSunken,
                        shape = RoundedCornerShape(AppTheme.radius.dp8),
                    )
                    .clickable(enabled = isSendEnabled, onClick = onSendClick),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    painter = AppIcons.send,
                    contentDescription = "Отправить",
                    size = IconSize.Medium,
                    tint = if (isSendEnabled) colors.accentOn else colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: AttachmentPreview,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.dp8)
    val isFailed = attachment.state == AttachmentState.Failed

    Box(
        modifier = Modifier
            .height(AppTheme.sizing.attachmentPreview)
            .background(color = colors.bgSurfaceSunken, shape = shape)
            .then(
                if (isFailed) {
                    Modifier.border(width = AppTheme.borders.field, color = colors.danger, shape = shape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = isFailed, onClick = onRetry),
    ) {
        if (attachment.isImage) {
            Box(modifier = Modifier.size(AppTheme.sizing.attachmentPreview))
        } else {
            Row(
                modifier = Modifier
                    .height(AppTheme.sizing.attachmentPreview)
                    .padding(horizontal = AppTheme.spacing.dp8),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    painter = AppIcons.attach,
                    contentDescription = null,
                    tint = colors.textSecondary,
                )
                Column {
                    Text(
                        text = attachment.fileName,
                        style = AppTheme.typography.caption,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                    Text(
                        text = attachment.sizeLabel,
                        style = AppTheme.typography.overline,
                        color = colors.textMuted,
                    )
                }
            }
        }
        when (attachment.state) {
            AttachmentState.Ready -> Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onRemove),
            ) {
                AppIcon(
                    painter = AppIcons.close,
                    contentDescription = "Убрать вложение",
                    size = IconSize.Small,
                    tint = colors.textSecondary,
                )
            }
            AttachmentState.Uploading -> Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = colors.textPrimary.copy(alpha = UPLOADING_SCRIM_ALPHA),
                        shape = shape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PROGRESS_SIZE),
                    color = colors.accentOn,
                )
            }
            AttachmentState.Failed -> Box(
                modifier = Modifier.align(Alignment.Center),
            ) {
                AppIcon(
                    painter = AppIcons.failed,
                    contentDescription = "Не загрузилось, повторить",
                    size = IconSize.Medium,
                    tint = colors.danger,
                )
            }
        }
    }
}
