package app.trainer.data.chat

import kotlinx.datetime.Instant

enum class MessageDelivery { PENDING, SENT, FAILED }

data class MessageAttachment(
    val id: String,
    val contentType: String,
    val sizeBytes: Long,
    val originalName: String,
)

data class PreparedUpload(
    val attachmentId: String,
    val uploadUrl: String,
)

data class Message(
    val clientMessageId: String,
    val serverId: String?,
    val dialogId: String,
    val seq: Long?,
    val senderUserId: String,
    val body: String,
    val createdAt: Instant,
    val delivery: MessageDelivery,
    val attachments: List<MessageAttachment>,
)
