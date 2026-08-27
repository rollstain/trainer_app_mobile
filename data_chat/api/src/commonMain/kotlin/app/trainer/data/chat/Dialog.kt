package app.trainer.data.chat

import kotlin.time.Instant

data class Dialog(
    val id: String,
    val coachId: String,
    val clientUserId: String,
    val peerUserId: String,
    val peerDisplayName: String,
    val lastMessageSeq: Long,
    val readSeq: Long,
    val peerReadSeq: Long,
    val unreadCount: Long,
    val lastMessagePreview: String?,
    val lastMessageAt: Instant?,
)
