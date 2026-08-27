package app.trainer.data.clients

import kotlin.time.Instant

enum class ClientNoteKind { MEDICAL, GENERAL }

data class ClientNote(
    val id: String,
    val clientUserId: String,
    val kind: ClientNoteKind,
    val title: String,
    val details: String?,
    val isPinned: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ClientNoteDraft(
    val kind: ClientNoteKind,
    val title: String,
    val details: String?,
    val isPinned: Boolean,
)
