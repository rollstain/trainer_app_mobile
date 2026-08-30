package app.trainer.data.chat.impl

import app.trainer.data.chat.ChatRepository
import app.trainer.data.chat.Dialog
import app.trainer.data.chat.Message
import app.trainer.data.chat.MessageAttachment
import app.trainer.data.chat.MessageDelivery
import app.trainer.data.chat.PreparedUpload
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.PresignedUploader
import app.trainer.network.safePagedRequest
import app.trainer.network.safeRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

private const val DIALOGS_PAGE_SIZE = 30

class ChatRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val localStore: ChatLocalStore,
    private val mapper: ChatMapper,
    private val presignedUploader: PresignedUploader,
) : ChatRepository {

    private val client get() = httpClientProvider.client

    private var nextDialogsCursor: String? = null

    override suspend fun clearLocalData() = localStore.clear()

    override fun observeDialogs(): Flow<List<Dialog>> = localStore.observeDialogs()

    override fun observeDialog(dialogId: String): Flow<Dialog?> = localStore.observeDialog(dialogId)

    override fun observeMessages(dialogId: String): Flow<List<Message>> = localStore.observeMessages(dialogId)

    override suspend fun refreshDialogs(): RequestResult<Boolean> = loadDialogs(after = null)

    override suspend fun loadMoreDialogs(): RequestResult<Boolean> {
        val cursor = nextDialogsCursor ?: return RequestResult.Success(false)
        return loadDialogs(after = cursor)
    }

    private suspend fun loadDialogs(after: String?): RequestResult<Boolean> {
        val loaded = safePagedRequest<List<DialogResponse>> {
            client.get("dialogs") {
                parameter("limit", DIALOGS_PAGE_SIZE)
                if (after != null) parameter("after", after)
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                localStore.storeDialogs(loaded.data.items.mapNotNull(mapper::toDialog))
                nextDialogsCursor = loaded.data.nextCursor
                RequestResult.Success(loaded.data.hasMore)
            }
        }
    }

    override suspend fun syncMessages(dialogId: String): RequestResult<Unit> {
        val knownSeq = localStore.maxSeqOf(dialogId)
        val loaded = safeRequest<List<MessageResponse>> {
            client.get("dialogs/$dialogId/messages/after") {
                parameter("afterSeq", knownSeq)
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                localStore.storeMessages(loaded.data.mapNotNull(mapper::toMessage))
                RequestResult.Success(Unit)
            }
        }
    }

    override suspend fun loadOlderMessages(dialogId: String): RequestResult<Boolean> {
        val oldestKnownSeq = localStore.minSeqOf(dialogId)
        val loaded = safeRequest<List<MessageResponse>> {
            client.get("dialogs/$dialogId/messages") {
                if (oldestKnownSeq != null) parameter("beforeSeq", oldestKnownSeq)
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                val older = loaded.data.mapNotNull(mapper::toMessage)
                localStore.storeMessages(older)
                RequestResult.Success(older.isNotEmpty())
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(
        dialogId: String,
        senderUserId: String,
        body: String,
        attachments: List<MessageAttachment>,
    ): RequestResult<Unit> {
        val pending = Message(
            clientMessageId = Uuid.random().toString(),
            serverId = null,
            dialogId = dialogId,
            seq = null,
            senderUserId = senderUserId,
            body = body,
            createdAt = Clock.System.now(),
            delivery = MessageDelivery.PENDING,
            attachments = attachments,
        )
        localStore.storeMessage(pending)
        return deliver(pending)
    }

    override suspend fun prepareUpload(
        dialogId: String,
        fileName: String,
        contentType: String,
        sizeBytes: Long,
    ): RequestResult<PreparedUpload> {
        val prepared = safeRequest<PrepareUploadResponse> {
            client.post("dialogs/$dialogId/attachments") {
                contentType(ContentType.Application.Json)
                setBody(
                    PrepareUploadRequest(
                        fileName = fileName,
                        contentType = contentType,
                        sizeBytes = sizeBytes,
                    )
                )
            }
        }
        return when (prepared) {
            is RequestResult.Error -> prepared
            is RequestResult.Success -> {
                val attachmentId = prepared.data.mediaFileId
                val uploadUrl = prepared.data.uploadUrl
                if (attachmentId == null || uploadUrl == null) {
                    RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "",
                        devMessage = "В ответе на подготовку загрузки нет mediaFileId или uploadUrl",
                    )
                } else {
                    RequestResult.Success(PreparedUpload(attachmentId = attachmentId, uploadUrl = uploadUrl))
                }
            }
        }
    }

    override suspend fun uploadFile(
        uploadUrl: String,
        contentType: String,
        bytes: ByteArray,
    ): RequestResult<Unit> {
        return presignedUploader.upload(uploadUrl = uploadUrl, contentType = contentType, bytes = bytes)
    }

    override suspend fun attachmentDownloadUrl(
        dialogId: String,
        attachmentId: String,
    ): RequestResult<String> {
        val loaded = safeRequest<AttachmentDownloadUrlResponse> {
            client.get("dialogs/$dialogId/attachments/$attachmentId/download-url")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                val downloadUrl = loaded.data.downloadUrl
                if (downloadUrl == null) {
                    RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "",
                        devMessage = "В ответе нет downloadUrl",
                    )
                } else {
                    RequestResult.Success(downloadUrl)
                }
            }
        }
    }

    override suspend fun retryPendingMessages(): RequestResult<Unit> {
        localStore.undeliveredMessages().forEach { message -> deliver(message) }
        return RequestResult.Success(Unit)
    }

    override suspend fun markRead(dialogId: String, readSeq: Long): RequestResult<Unit> {
        val marked = safeRequest<Unit> {
            client.post("dialogs/$dialogId/read") {
                contentType(ContentType.Application.Json)
                setBody(MarkReadRequest(readSeq = readSeq))
            }
        }
        if (marked is RequestResult.Success) {
            localStore.markRead(dialogId = dialogId, readSeq = readSeq)
        }
        return marked
    }

    private suspend fun deliver(message: Message): RequestResult<Unit> {
        val sent = safeRequest<MessageResponse> {
            client.post("dialogs/${message.dialogId}/messages") {
                contentType(ContentType.Application.Json)
                setBody(
                    SendMessageRequest(
                        clientMessageId = message.clientMessageId,
                        body = message.body,
                        attachmentIds = message.attachments.map { it.id },
                    )
                )
            }
        }
        return when (sent) {
            is RequestResult.Error -> {
                localStore.updateDelivery(
                    clientMessageId = message.clientMessageId,
                    delivery = MessageDelivery.FAILED,
                )
                sent
            }
            is RequestResult.Success -> {
                val confirmed = mapper.toMessage(sent.data)
                if (confirmed == null) {
                    localStore.updateDelivery(
                        clientMessageId = message.clientMessageId,
                        delivery = MessageDelivery.FAILED,
                    )
                } else {
                    localStore.storeMessage(confirmed)
                }
                RequestResult.Success(Unit)
            }
        }
    }
}
