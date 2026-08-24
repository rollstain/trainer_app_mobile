package app.trainer.data.chat.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.trainer.data.chat.Dialog
import app.trainer.data.chat.Message
import app.trainer.data.chat.MessageAttachment
import app.trainer.data.chat.MessageDelivery
import app.trainer.database.TrainerDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatLocalStore(
    private val database: TrainerDatabase,
    private val mapper: ChatMapper,
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val queries get() = database.chatQueries

    fun observeDialogs(): Flow<List<Dialog>> {
        return queries.selectDialogs()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { stored -> stored.map(mapper::toDialog) }
    }

    fun observeDialog(dialogId: String): Flow<Dialog?> {
        return queries.selectDialogById(dialogId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { stored -> stored?.let(mapper::toDialog) }
    }

    fun observeMessages(dialogId: String): Flow<List<Message>> {
        val messages = queries.selectMessages(dialogId = dialogId)
            .asFlow()
            .mapToList(ioDispatcher)
        val attachments = queries.selectAttachmentsOfDialog(dialogId)
            .asFlow()
            .mapToList(ioDispatcher)
        return combine(messages, attachments) { storedMessages, storedAttachments ->
            val attachmentsByMessage = storedAttachments
                .groupBy { it.clientMessageId }
                .mapValues { (_, rows) -> rows.map(mapper::toAttachment) }
            storedMessages.map { stored ->
                mapper.toMessage(
                    stored = stored,
                    attachments = attachmentsByMessage[stored.clientMessageId].orEmpty(),
                )
            }
        }
    }

    suspend fun maxSeqOf(dialogId: String): Long = withContext(ioDispatcher) {
        queries.maxSeqInDialog(dialogId).executeAsOne().MAX ?: 0L
    }

    suspend fun minSeqOf(dialogId: String): Long? = withContext(ioDispatcher) {
        queries.minSeqInDialog(dialogId).executeAsOne().MIN
    }

    suspend fun undeliveredMessages(): List<Message> = withContext(ioDispatcher) {
        val pending = queries.selectPendingMessages(MessageDelivery.PENDING.name).executeAsList()
        val failed = queries.selectPendingMessages(MessageDelivery.FAILED.name).executeAsList()
        (pending + failed).map { stored ->
            val attachments = queries.selectAttachmentsOfMessage(stored.clientMessageId)
                .executeAsList()
                .map(mapper::toAttachment)
            mapper.toMessage(stored = stored, attachments = attachments)
        }
    }

    suspend fun storeDialogs(dialogs: List<Dialog>) {
        withContext(ioDispatcher) {
            queries.transaction {
                dialogs.forEach(::upsertDialog)
            }
        }
    }

    suspend fun storeMessage(message: Message) {
        withContext(ioDispatcher) {
            queries.transaction {
                upsertMessage(message)
            }
        }
    }

    suspend fun storeMessages(messages: List<Message>) {
        withContext(ioDispatcher) {
            queries.transaction {
                messages.forEach(::upsertMessage)
            }
        }
    }

    suspend fun updateDelivery(clientMessageId: String, delivery: MessageDelivery) {
        withContext(ioDispatcher) {
            queries.updateDeliveryStatus(deliveryStatus = delivery.name, clientMessageId = clientMessageId)
        }
    }

    suspend fun clear() {
        withContext(ioDispatcher) {
            queries.transaction {
                queries.deleteAllAttachments()
                queries.deleteAllMessages()
                queries.deleteAllDialogs()
            }
        }
    }

    suspend fun markRead(dialogId: String, readSeq: Long) {
        withContext(ioDispatcher) {
            queries.markDialogRead(readSeq = readSeq, id = dialogId)
        }
    }

    private fun upsertDialog(dialog: Dialog) {
        queries.upsertDialog(
            id = dialog.id,
            coachId = dialog.coachId,
            clientUserId = dialog.clientUserId,
            peerUserId = dialog.peerUserId,
            peerDisplayName = dialog.peerDisplayName,
            lastMessageSeq = dialog.lastMessageSeq,
            readSeq = dialog.readSeq,
            peerReadSeq = dialog.peerReadSeq,
            unreadCount = dialog.unreadCount,
            lastMessagePreview = dialog.lastMessagePreview,
            lastMessageAtEpochMs = dialog.lastMessageAt?.toEpochMilliseconds(),
        )
    }

    private fun upsertMessage(message: Message) {
        queries.upsertMessage(
            clientMessageId = message.clientMessageId,
            serverId = message.serverId,
            dialogId = message.dialogId,
            seq = message.seq,
            senderUserId = message.senderUserId,
            body = message.body,
            createdAtEpochMs = message.createdAt.toEpochMilliseconds(),
            deliveryStatus = message.delivery.name,
        )
        message.attachments.forEach { attachment -> upsertAttachment(message.clientMessageId, attachment) }
    }

    private fun upsertAttachment(clientMessageId: String, attachment: MessageAttachment) {
        queries.upsertAttachment(
            id = attachment.id,
            clientMessageId = clientMessageId,
            contentType = attachment.contentType,
            sizeBytes = attachment.sizeBytes,
            originalName = attachment.originalName,
        )
    }
}
