package app.trainer.feature.chat.presentation.dialog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.trainer.feature.chat.presentation.dialog.mvi.ChatItem
import app.trainer.feature.chat.presentation.dialog.mvi.DialogEvent
import app.trainer.feature.chat.presentation.dialog.mvi.DialogState
import app.trainer.feature.chat.presentation.dialog.mvi.MessageRow
import app.trainer.media.rememberImagePicker
import app.trainer.feature.chat.presentation.dialog.mvi.MessageStatus
import app.trainer.feature.chat.presentation.dialog.mvi.PendingAttachment
import app.trainer.feature.chat.presentation.dialog.mvi.PendingAttachmentState
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppMessageBubble
import app.trainer.uikit.widgets.AppMessageInput
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AttachmentPreview
import app.trainer.uikit.widgets.AttachmentState
import app.trainer.uikit.widgets.BubbleAttachment
import app.trainer.uikit.widgets.MessageDeliveryState
import app.trainer.uikit.widgets.MessageSide
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.TopBarSubtitle

private const val PEER_ROLE = "подопечный"
private const val LOADING_OLDER = "Загружаем более ранние…"
private const val EMPTY_TITLE = "Сообщений пока нет"
private const val EMPTY_DESCRIPTION = "Напишите первым — сообщение уйдёт, как только появится сеть."
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION =
    "Проверьте соединение. Отправленные сообщения останутся в очереди и уйдут сами."
private const val FAILURE_ACTION = "Повторить"

@Composable
fun DialogView(
    modifier: Modifier = Modifier,
    state: DialogState,
    onEvent: (DialogEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = state.peerDisplayName,
            leading = TopBarLeading.Back(onClick = onBackClick),
            subtitle = TopBarSubtitle.Text(PEER_ROLE),
            avatarName = state.peerDisplayName,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isFailed && state.items.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(DialogEvent.OnRetryClicked) },
                    ),
                )
                state.items.isEmpty() && !state.isLoading -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EMPTY_TITLE,
                    description = EMPTY_DESCRIPTION,
                )
                else -> MessageFeed(state = state, onEvent = onEvent)
            }
        }
        val picker = rememberImagePicker { image ->
            onEvent(
                DialogEvent.OnFileAttached(
                    fileName = image.fileName,
                    contentType = image.contentType,
                    bytes = image.bytes,
                )
            )
        }
        AppMessageInput(
            value = state.draft,
            onValueChange = { onEvent(DialogEvent.OnDraftChanged(it)) },
            attachments = state.pendingAttachments.map(::toPreview),
            onAttachClick = picker::pick,
            onSendClick = { onEvent(DialogEvent.OnSendClicked) },
            onAttachmentRemove = { onEvent(DialogEvent.OnPendingAttachmentRemoved(it)) },
            onAttachmentRetry = { onEvent(DialogEvent.OnFailedMessagesRetried) },
        )
    }
}

@Composable
private fun MessageFeed(state: DialogState, onEvent: (DialogEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        if (state.hasMoreHistory) {
            item(key = "load-older") {
                LoadOlderIndicator(onAppear = { onEvent(DialogEvent.OnOlderMessagesRequested) })
            }
        }
        items(items = state.items, key = ::itemKey) { item ->
            when (item) {
                is ChatItem.DayDivider -> DayDivider(label = item.label)
                is ChatItem.Message -> MessageItem(row = item.row, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun MessageItem(row: MessageRow, onEvent: (DialogEvent) -> Unit) {
    AppMessageBubble(
        text = row.body,
        time = row.timeLabel,
        side = if (row.isMine) MessageSide.Own else MessageSide.Other,
        delivery = toDeliveryState(row.status),
        onRetryClick = { onEvent(DialogEvent.OnFailedMessagesRetried) },
        attachments = row.attachments.map { attachment ->
            BubbleAttachment(id = attachment.attachmentId, url = attachment.url)
        },
    )
}

@Composable
private fun DayDivider(label: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AppText(
            modifier = Modifier
                .background(
                    color = AppTheme.colors.bgSurfaceSunken,
                    shape = RoundedCornerShape(AppTheme.radius.pill),
                )
                .padding(horizontal = AppTheme.spacing.dp12, vertical = AppTheme.spacing.dp4),
            text = label,
            style = AppTheme.typography.overline,
            color = AppTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun LoadOlderIndicator(onAppear: () -> Unit) {
    LaunchedEffect(Unit) { onAppear() }
    Box(
        modifier = Modifier.fillMaxWidth().height(AppTheme.sizing.offlineBannerHeight),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = LOADING_OLDER,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun itemKey(item: ChatItem): String = when (item) {
    is ChatItem.DayDivider -> "day-${item.label}"
    is ChatItem.Message -> item.row.clientMessageId
}

private fun toDeliveryState(status: MessageStatus): MessageDeliveryState = when (status) {
    MessageStatus.Pending -> MessageDeliveryState.Pending
    MessageStatus.Sent -> MessageDeliveryState.Sent
    MessageStatus.Read -> MessageDeliveryState.Read
    MessageStatus.Failed -> MessageDeliveryState.Failed
}

private fun toPreview(attachment: PendingAttachment): AttachmentPreview = AttachmentPreview(
    id = attachment.attachmentId,
    fileName = attachment.originalName,
    sizeLabel = attachment.sizeLabel,
    isImage = attachment.isImage,
    state = when (attachment.state) {
        PendingAttachmentState.Uploading -> AttachmentState.Uploading
        PendingAttachmentState.Ready -> AttachmentState.Ready
        PendingAttachmentState.Failed -> AttachmentState.Failed
    },
)
