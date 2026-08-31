package app.trainer.feature.chat.presentation.dialog.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AttachmentRow(
    val attachmentId: String,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val url: String?,
)

enum class MessageStatus { Pending, Sent, Read, Failed }

data class MessageRow(
    val clientMessageId: String,
    val body: String,
    val timeLabel: String,
    val isMine: Boolean,
    val status: MessageStatus,
    val attachments: ImmutableList<AttachmentRow>,
)

sealed interface ChatItem {

    data class DayDivider(val label: String) : ChatItem

    data class Message(val row: MessageRow) : ChatItem
}

enum class PendingAttachmentState { Uploading, Ready, Failed }

data class PendingAttachment(
    val attachmentId: String,
    val originalName: String,
    val contentType: String,
    val sizeLabel: String,
    val isImage: Boolean,
    val state: PendingAttachmentState,
)

data class DialogState(
    val peerDisplayName: String,
    val peerRoleLabel: String,
    val items: ImmutableList<ChatItem>,
    val draft: String,
    val pendingAttachments: ImmutableList<PendingAttachment>,
    val isLoading: Boolean,
    val isSending: Boolean,
    val isUploading: Boolean,
    val isLoadingOlder: Boolean,
    val hasMoreHistory: Boolean,
    val failure: RequestResult.Error?,
) {

    val isSendEnabled: Boolean
        get() = !isSending &&
            !isUploading &&
            (draft.isNotBlank() || pendingAttachments.any { it.state == PendingAttachmentState.Ready })

    companion object {

        fun initial(): DialogState = DialogState(
            peerDisplayName = "",
            peerRoleLabel = "",
            items = persistentListOf(),
            draft = "",
            pendingAttachments = persistentListOf(),
            isLoading = true,
            isSending = false,
            isUploading = false,
            isLoadingOlder = false,
            hasMoreHistory = true,
            failure = null,
        )
    }
}

sealed interface DialogEvent {

    data object OnRetryClicked : DialogEvent

    data object OnSendClicked : DialogEvent

    data class OnDraftChanged(val draft: String) : DialogEvent

    data object OnFailedMessagesRetried : DialogEvent

    data object OnOlderMessagesRequested : DialogEvent

    class OnFileAttached(
        val fileName: String,
        val contentType: String,
        val bytes: ByteArray,
    ) : DialogEvent

    data class OnPendingAttachmentRemoved(val attachmentId: String) : DialogEvent

    data class OnPendingAttachmentRetried(val attachmentId: String) : DialogEvent

    data class OnAttachmentOpened(val attachmentId: String) : DialogEvent
}

sealed interface DialogSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : DialogSideEffect

    data object ScrollToLatest : DialogSideEffect

    data class OpenAttachment(val downloadUrl: String) : DialogSideEffect
}
