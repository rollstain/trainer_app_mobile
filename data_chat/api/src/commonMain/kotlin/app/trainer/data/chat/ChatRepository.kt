package app.trainer.data.chat

import app.trainer.entities.LocalDataCleaner
import app.trainer.entities.RequestResult
import kotlinx.coroutines.flow.Flow

interface ChatRepository : LocalDataCleaner {

    fun observeDialogs(): Flow<List<Dialog>>

    fun observeDialog(dialogId: String): Flow<Dialog?>

    fun observeMessages(dialogId: String): Flow<List<Message>>

    suspend fun refreshDialogs(): RequestResult<Boolean>

    suspend fun loadMoreDialogs(): RequestResult<Boolean>

    suspend fun syncMessages(dialogId: String): RequestResult<Unit>

    suspend fun loadOlderMessages(dialogId: String): RequestResult<Boolean>

    suspend fun sendMessage(
        dialogId: String,
        senderUserId: String,
        body: String,
        attachments: List<MessageAttachment>,
    ): RequestResult<Unit>

    suspend fun prepareUpload(
        dialogId: String,
        fileName: String,
        contentType: String,
        sizeBytes: Long,
    ): RequestResult<PreparedUpload>

    suspend fun uploadFile(uploadUrl: String, contentType: String, bytes: ByteArray): RequestResult<Unit>

    suspend fun attachmentDownloadUrl(dialogId: String, attachmentId: String): RequestResult<String>

    suspend fun retryPendingMessages(): RequestResult<Unit>

    suspend fun markRead(dialogId: String, readSeq: Long): RequestResult<Unit>
}
