package app.trainer.data.chat.impl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    @SerialName("clientMessageId")
    val clientMessageId: String,
    @SerialName("body")
    val body: String,
    @SerialName("attachmentIds")
    val attachmentIds: List<String>,
)

@Serializable
data class PrepareUploadRequest(
    @SerialName("fileName")
    val fileName: String,
    @SerialName("contentType")
    val contentType: String,
    @SerialName("sizeBytes")
    val sizeBytes: Long,
)

@Serializable
data class PrepareUploadResponse(
    @SerialName("mediaFileId")
    val mediaFileId: String?,
    @SerialName("uploadUrl")
    val uploadUrl: String?,
)

@Serializable
data class AttachmentDownloadUrlResponse(
    @SerialName("downloadUrl")
    val downloadUrl: String?,
)

@Serializable
data class AttachmentResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("contentType")
    val contentType: String?,
    @SerialName("sizeBytes")
    val sizeBytes: Long?,
    @SerialName("originalName")
    val originalName: String?,
)

@Serializable
data class MarkReadRequest(
    @SerialName("readSeq")
    val readSeq: Long,
)

@Serializable
data class MessageResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("dialogId")
    val dialogId: String?,
    @SerialName("seq")
    val seq: Long?,
    @SerialName("senderUserId")
    val senderUserId: String?,
    @SerialName("clientMessageId")
    val clientMessageId: String?,
    @SerialName("body")
    val body: String?,
    @SerialName("createdAt")
    val createdAt: String?,
    @SerialName("attachments")
    val attachments: List<AttachmentResponse>?,
)

@Serializable
data class DialogResponse(
    @SerialName("id")
    val id: String?,
    @SerialName("coachId")
    val coachId: String?,
    @SerialName("clientUserId")
    val clientUserId: String?,
    @SerialName("peerUserId")
    val peerUserId: String?,
    @SerialName("peerDisplayName")
    val peerDisplayName: String?,
    @SerialName("lastMessageSeq")
    val lastMessageSeq: Long?,
    @SerialName("readSeq")
    val readSeq: Long?,
    @SerialName("peerReadSeq")
    val peerReadSeq: Long?,
    @SerialName("unreadCount")
    val unreadCount: Long?,
    @SerialName("lastMessagePreview")
    val lastMessagePreview: String?,
    @SerialName("lastMessageAt")
    val lastMessageAt: String?,
)
