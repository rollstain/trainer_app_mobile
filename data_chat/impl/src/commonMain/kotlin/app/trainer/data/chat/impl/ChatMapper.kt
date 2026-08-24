package app.trainer.data.chat.impl

import app.trainer.data.chat.Dialog
import app.trainer.data.chat.Message
import app.trainer.data.chat.MessageAttachment
import app.trainer.data.chat.MessageDelivery
import app.trainer.database.AttachmentEntity
import app.trainer.database.MessageEntity
import app.trainer.database.DialogEntity
import app.trainer.logger.Logger
import kotlinx.datetime.Instant

private const val LOG_TAG = "chat-mapper"

class ChatMapper(private val logger: Logger) {

    fun toMessage(response: MessageResponse): Message? {
        val clientMessageId = response.clientMessageId ?: return skipped(field = "clientMessageId")
        val dialogId = response.dialogId ?: return skipped(field = "dialogId")
        val senderUserId = response.senderUserId ?: return skipped(field = "senderUserId")
        val body = response.body ?: return skipped(field = "body")
        val createdAt = response.createdAt?.let(::parseInstant) ?: return skipped(field = "createdAt")
        return Message(
            clientMessageId = clientMessageId,
            serverId = response.id,
            dialogId = dialogId,
            seq = response.seq,
            senderUserId = senderUserId,
            body = body,
            createdAt = createdAt,
            delivery = MessageDelivery.SENT,
            attachments = response.attachments.orEmpty().mapNotNull(::toAttachment),
        )
    }

    fun toDialog(response: DialogResponse): Dialog? {
        val id = response.id ?: return skipped(field = "id")
        val coachId = response.coachId ?: return skipped(field = "coachId")
        val clientUserId = response.clientUserId ?: return skipped(field = "clientUserId")
        val peerUserId = response.peerUserId ?: return skipped(field = "peerUserId")
        val peerDisplayName = response.peerDisplayName ?: return skipped(field = "peerDisplayName")
        val lastMessage = response.lastMessage?.let(::toMessage)
        return Dialog(
            id = id,
            coachId = coachId,
            clientUserId = clientUserId,
            peerUserId = peerUserId,
            peerDisplayName = peerDisplayName,
            lastMessageSeq = response.lastMessageSeq ?: 0,
            readSeq = response.readSeq ?: 0,
            peerReadSeq = response.peerReadSeq ?: 0,
            unreadCount = response.unreadCount ?: 0,
            lastMessagePreview = lastMessage?.body,
            lastMessageAt = lastMessage?.createdAt,
        )
    }

    fun toAttachment(response: AttachmentResponse): MessageAttachment? {
        val id = response.id ?: return skipped(field = "attachment.id")
        val contentType = response.contentType ?: return skipped(field = "attachment.contentType")
        val sizeBytes = response.sizeBytes ?: return skipped(field = "attachment.sizeBytes")
        val originalName = response.originalName ?: return skipped(field = "attachment.originalName")
        return MessageAttachment(
            id = id,
            contentType = contentType,
            sizeBytes = sizeBytes,
            originalName = originalName,
        )
    }

    fun toAttachment(stored: AttachmentEntity): MessageAttachment = MessageAttachment(
        id = stored.id,
        contentType = stored.contentType,
        sizeBytes = stored.sizeBytes,
        originalName = stored.originalName,
    )

    fun toMessage(stored: MessageEntity, attachments: List<MessageAttachment>): Message = Message(
        clientMessageId = stored.clientMessageId,
        serverId = stored.serverId,
        dialogId = stored.dialogId,
        seq = stored.seq,
        senderUserId = stored.senderUserId,
        body = stored.body,
        createdAt = Instant.fromEpochMilliseconds(stored.createdAtEpochMs),
        delivery = toDelivery(stored.deliveryStatus),
        attachments = attachments,
    )

    fun toDialog(stored: DialogEntity): Dialog = Dialog(
        id = stored.id,
        coachId = stored.coachId,
        clientUserId = stored.clientUserId,
        peerUserId = stored.peerUserId,
        peerDisplayName = stored.peerDisplayName,
        lastMessageSeq = stored.lastMessageSeq,
        readSeq = stored.readSeq,
        peerReadSeq = stored.peerReadSeq,
        unreadCount = stored.unreadCount,
        lastMessagePreview = stored.lastMessagePreview,
        lastMessageAt = stored.lastMessageAtEpochMs?.let(Instant::fromEpochMilliseconds),
    )

    private fun toDelivery(stored: String): MessageDelivery {
        val known = MessageDelivery.entries.firstOrNull { it.name == stored }
        if (known != null) return known
        logger.error(tag = LOG_TAG, message = "Неизвестный deliveryStatus=$stored, считаем FAILED")
        return MessageDelivery.FAILED
    }

    private fun parseInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }.getOrNull()
    }

    private fun <T> skipped(field: String): T? {
        logger.error(tag = LOG_TAG, message = "Пропущено сообщение: в ответе нет поля $field")
        return null
    }
}
