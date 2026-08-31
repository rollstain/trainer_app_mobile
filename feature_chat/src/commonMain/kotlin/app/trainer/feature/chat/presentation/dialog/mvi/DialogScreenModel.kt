package app.trainer.feature.chat.presentation.dialog.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.monthGenitiveOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.data.chat.ChatRepository
import app.trainer.data.chat.Dialog
import app.trainer.data.chat.Message
import app.trainer.data.chat.MessageAttachment
import app.trainer.data.chat.MessageDelivery
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.attachment_size_bytes
import app.trainer.strings.attachment_size_kilobytes
import app.trainer.strings.attachment_size_megabytes
import app.trainer.strings.dialog_client_role
import app.trainer.strings.dialog_coach_role
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private const val IMAGE_CONTENT_TYPE_PREFIX = "image/"
private const val BYTES_IN_KILOBYTE = 1024
private const val BYTES_IN_MEGABYTE = 1024 * 1024

class DialogScreenModel(
    private val dialogId: String,
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<DialogState, DialogSideEffect, DialogEvent>(
    initialState = DialogState.initial(),
) {

    private var currentUserId: String? = null
    private val uploadedSizes = mutableMapOf<String, Long>()
    private val pendingUploads = mutableMapOf<String, PendingUpload>()
    private val attachmentUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    private val requestedAttachmentIds = mutableSetOf<String>()

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            if (!loadCurrentUser()) return@onFetchDataScope
            observeDialog()
        }
        syncMessages()
    }

    override fun dispatch(event: DialogEvent) {
        when (event) {
            DialogEvent.OnRetryClicked -> onFetchData()
            DialogEvent.OnSendClicked -> sendDraft()
            DialogEvent.OnFailedMessagesRetried -> retryUndelivered()
            DialogEvent.OnOlderMessagesRequested -> loadOlderMessages()
            is DialogEvent.OnDraftChanged -> updateState { it.copy(draft = event.draft) }
            is DialogEvent.OnFileAttached -> attachFile(event)
            is DialogEvent.OnPendingAttachmentRemoved -> removePendingAttachment(event.attachmentId)
            is DialogEvent.OnPendingAttachmentRetried -> retryUpload(event.attachmentId)
            is DialogEvent.OnAttachmentOpened -> openAttachment(event.attachmentId)
        }
    }

    private suspend fun loadCurrentUser(): Boolean {
        if (currentUserId != null) return true
        return when (val profile = profileRepository.me()) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = profile) }
                postSideEffect(DialogSideEffect.ShowFailure(profile))
                false
            }
            is RequestResult.Success -> {
                currentUserId = profile.data.userId
                true
            }
        }
    }

    private suspend fun observeDialog() {
        combine(
            chatRepository.observeDialog(dialogId),
            chatRepository.observeMessages(dialogId),
            attachmentUrls,
        ) { dialog, messages, urls -> Triple(dialog, messages, urls) }
            .collectLatest { (dialog, messages, urls) ->
                val peerRoleLabel = dialog?.let { toPeerRoleLabel(it) }
                val items = toItems(
                    messages = messages,
                    peerReadSeq = dialog?.peerReadSeq ?: 0,
                    urls = urls,
                ).toImmutableList()
                updateState { current ->
                    current.copy(
                        peerDisplayName = dialog?.peerDisplayName ?: current.peerDisplayName,
                        peerRoleLabel = peerRoleLabel ?: current.peerRoleLabel,
                        items = items,
                        isLoading = false,
                    )
                }
                markReadUpTo(messages)
                resolveAttachmentUrls(messages)
            }
    }

    private suspend fun markReadUpTo(messages: List<Message>) {
        val latestSeq = messages.mapNotNull { it.seq }.maxOrNull() ?: return
        chatRepository.markRead(dialogId = dialogId, readSeq = latestSeq)
    }

    private fun syncMessages() {
        screenModelScope {
            when (val synced = chatRepository.syncMessages(dialogId = dialogId)) {
                is RequestResult.Error -> {
                    updateState { current ->
                        current.copy(failure = synced.takeIf { current.items.isEmpty() })
                    }
                    postSideEffect(DialogSideEffect.ShowFailure(synced))
                }
                is RequestResult.Success -> {
                    chatRepository.refreshDialogs()
                    updateState { it.copy(failure = null) }
                }
            }
        }
    }

    private fun loadOlderMessages() {
        screenModelScope { state ->
            if (state.isLoadingOlder || !state.hasMoreHistory) return@screenModelScope
            updateState { it.copy(isLoadingOlder = true) }
            when (val loaded = chatRepository.loadOlderMessages(dialogId = dialogId)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingOlder = false) }
                    postSideEffect(DialogSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> updateState {
                    it.copy(isLoadingOlder = false, hasMoreHistory = loaded.data)
                }
            }
        }
    }

    private fun attachFile(event: DialogEvent.OnFileAttached) {
        screenModelScope {
            updateState { it.copy(isUploading = true) }
            val prepared = chatRepository.prepareUpload(
                dialogId = dialogId,
                fileName = event.fileName,
                contentType = event.contentType,
                sizeBytes = event.bytes.size.toLong(),
            )
            if (prepared is RequestResult.Error) {
                updateState { it.copy(isUploading = false) }
                postSideEffect(DialogSideEffect.ShowFailure(prepared))
                return@screenModelScope
            }
            val upload = (prepared as RequestResult.Success).data
            val pending = PendingUpload(
                uploadUrl = upload.uploadUrl,
                contentType = event.contentType,
                bytes = event.bytes,
            )
            pendingUploads[upload.attachmentId] = pending
            uploadedSizes[upload.attachmentId] = event.bytes.size.toLong()
            val sizeLabel = formatSize(event.bytes.size.toLong())
            updateState { current ->
                current.copy(
                    pendingAttachments = (
                        current.pendingAttachments + PendingAttachment(
                            attachmentId = upload.attachmentId,
                            originalName = event.fileName,
                            contentType = event.contentType,
                            sizeLabel = sizeLabel,
                            isImage = event.contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX),
                            state = PendingAttachmentState.Uploading,
                        )
                        ).toImmutableList()
                )
            }
            sendToStorage(attachmentId = upload.attachmentId, upload = pending)
        }
    }

    private fun retryUpload(attachmentId: String) {
        val pending = pendingUploads[attachmentId] ?: return
        screenModelScope {
            updateState { current ->
                current.copy(
                    isUploading = true,
                    pendingAttachments = current.pendingAttachments
                        .withState(attachmentId = attachmentId, state = PendingAttachmentState.Uploading),
                )
            }
            sendToStorage(attachmentId = attachmentId, upload = pending)
        }
    }

    private suspend fun sendToStorage(attachmentId: String, upload: PendingUpload) {
        val uploaded = chatRepository.uploadFile(
            uploadUrl = upload.uploadUrl,
            contentType = upload.contentType,
            bytes = upload.bytes,
        )
        val state = when (uploaded) {
            is RequestResult.Error -> PendingAttachmentState.Failed
            is RequestResult.Success -> PendingAttachmentState.Ready
        }
        updateState { current ->
            current.copy(
                isUploading = false,
                pendingAttachments = current.pendingAttachments
                    .withState(attachmentId = attachmentId, state = state),
            )
        }
        if (uploaded is RequestResult.Error) postSideEffect(DialogSideEffect.ShowFailure(uploaded))
    }

    private fun removePendingAttachment(attachmentId: String) {
        pendingUploads.remove(attachmentId)
        updateState { current ->
            current.copy(
                pendingAttachments = current.pendingAttachments
                    .filterNot { it.attachmentId == attachmentId }
                    .toImmutableList()
            )
        }
    }

    private fun openAttachment(attachmentId: String) {
        screenModelScope {
            val url = chatRepository.attachmentDownloadUrl(dialogId = dialogId, attachmentId = attachmentId)
            when (url) {
                is RequestResult.Error -> postSideEffect(DialogSideEffect.ShowFailure(url))
                is RequestResult.Success -> postSideEffect(DialogSideEffect.OpenAttachment(url.data))
            }
        }
    }

    private fun sendDraft() {
        screenModelScope { state ->
            val body = state.draft.trim()
            val senderUserId = currentUserId
            if (senderUserId == null) return@screenModelScope
            val uploaded = state.pendingAttachments.filter { it.state == PendingAttachmentState.Ready }
            if (body.isEmpty() && uploaded.isEmpty()) return@screenModelScope

            val attachments = uploaded.map { pending ->
                MessageAttachment(
                    id = pending.attachmentId,
                    contentType = pending.contentType,
                    sizeBytes = uploadedSizes[pending.attachmentId] ?: 0,
                    originalName = pending.originalName,
                )
            }
            pendingUploads.clear()
            updateState { it.copy(draft = "", pendingAttachments = persistentListOf(), isSending = true) }
            val sent = chatRepository.sendMessage(
                dialogId = dialogId,
                senderUserId = senderUserId,
                body = body,
                attachments = attachments,
            )
            updateState { it.copy(isSending = false) }
            when (sent) {
                is RequestResult.Error -> postSideEffect(DialogSideEffect.ShowFailure(sent))
                is RequestResult.Success -> postSideEffect(DialogSideEffect.ScrollToLatest)
            }
        }
    }

    private fun retryUndelivered() {
        screenModelScope {
            chatRepository.retryPendingMessages()
        }
    }

    private fun resolveAttachmentUrls(messages: List<Message>) {
        val missing = messages
            .flatMap { it.attachments }
            .map { it.id }
            .distinct()
            .filterNot(requestedAttachmentIds::contains)
        if (missing.isEmpty()) return
        requestedAttachmentIds.addAll(missing)
        screenModelScope {
            val resolved = missing.mapNotNull { attachmentId ->
                val url = chatRepository.attachmentDownloadUrl(
                    dialogId = dialogId,
                    attachmentId = attachmentId,
                )
                if (url is RequestResult.Success) attachmentId to url.data else null
            }
            if (resolved.isNotEmpty()) attachmentUrls.value = attachmentUrls.value + resolved
        }
    }

    private suspend fun toPeerRoleLabel(dialog: Dialog): String {
        val role = if (dialog.peerUserId == dialog.clientUserId) {
            Res.string.dialog_client_role
        } else {
            Res.string.dialog_coach_role
        }
        return getString(role)
    }

    private suspend fun toItems(
        messages: List<Message>,
        peerReadSeq: Long,
        urls: Map<String, String>,
    ): List<ChatItem> {
        val zone = TimeZone.currentSystemDefault()
        val items = mutableListOf<ChatItem>()
        var previousDate: LocalDate? = null
        messages.sortedBy { it.createdAt }.forEach { message ->
            val date = message.createdAt.toLocalDateTime(zone).date
            if (date != previousDate) {
                items.add(ChatItem.DayDivider(label = formatDay(date)))
                previousDate = date
            }
            items.add(
                ChatItem.Message(
                    row = toRow(
                        message = message,
                        zone = zone,
                        peerReadSeq = peerReadSeq,
                        urls = urls,
                    ),
                )
            )
        }
        return items
    }

    private fun toRow(
        message: Message,
        zone: TimeZone,
        peerReadSeq: Long,
        urls: Map<String, String>,
    ): MessageRow = MessageRow(
        clientMessageId = message.clientMessageId,
        body = message.body,
        timeLabel = formatTimeOfDay(instant = message.createdAt, zone = zone),
        isMine = message.senderUserId == currentUserId,
        status = toStatus(message = message, peerReadSeq = peerReadSeq),
        attachments = message.attachments
            .map { attachment -> toAttachmentRow(attachment = attachment, url = urls[attachment.id]) }
            .toImmutableList(),
    )

    private fun toStatus(message: Message, peerReadSeq: Long): MessageStatus = when (message.delivery) {
        MessageDelivery.PENDING -> MessageStatus.Pending
        MessageDelivery.FAILED -> MessageStatus.Failed
        MessageDelivery.SENT -> {
            val seq = message.seq
            if (seq != null && seq <= peerReadSeq) MessageStatus.Read else MessageStatus.Sent
        }
    }

    private fun formatTimeOfDay(instant: Instant, zone: TimeZone): String =
        timeOfDayOf(instant.toLocalDateTime(zone))

    private suspend fun formatSize(sizeBytes: Long): String = when {
        sizeBytes >= BYTES_IN_MEGABYTE ->
            getString(Res.string.attachment_size_megabytes, sizeBytes / BYTES_IN_MEGABYTE)
        sizeBytes >= BYTES_IN_KILOBYTE ->
            getString(Res.string.attachment_size_kilobytes, sizeBytes / BYTES_IN_KILOBYTE)
        else -> getString(Res.string.attachment_size_bytes, sizeBytes)
    }

    private suspend fun formatDay(date: LocalDate): String {
        val month = monthGenitiveOf(date)
        return "${date.day} $month"
    }

    private fun toAttachmentRow(attachment: MessageAttachment, url: String?): AttachmentRow = AttachmentRow(
        attachmentId = attachment.id,
        originalName = attachment.originalName,
        contentType = attachment.contentType,
        sizeBytes = attachment.sizeBytes,
        url = url,
    )
}

private class PendingUpload(
    val uploadUrl: String,
    val contentType: String,
    val bytes: ByteArray,
)

private fun List<PendingAttachment>.withState(
    attachmentId: String,
    state: PendingAttachmentState,
) = map { if (it.attachmentId == attachmentId) it.copy(state = state) else it }.toImmutableList()
