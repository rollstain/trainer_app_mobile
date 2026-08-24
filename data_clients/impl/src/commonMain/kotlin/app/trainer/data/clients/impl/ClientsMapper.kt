package app.trainer.data.clients.impl

import app.trainer.data.clients.ClientNote
import app.trainer.data.clients.ClientNoteKind
import app.trainer.data.clients.CoachClient
import app.trainer.data.clients.CoachSummary
import app.trainer.logger.Logger
import kotlinx.datetime.Instant

private const val LOG_TAG = "clients-mapper"

class ClientsMapper(private val logger: Logger) {

    fun toNote(response: ClientNoteResponse): ClientNote? {
        val id = response.id ?: return skipped(entity = "ClientNote", field = "id")
        val clientUserId = response.clientUserId ?: return skipped(entity = "ClientNote", field = "clientUserId")
        val kind = ClientNoteKind.entries.firstOrNull { it.name == response.kind }
            ?: return skipped(entity = "ClientNote", field = "kind")
        val title = response.title ?: return skipped(entity = "ClientNote", field = "title")
        val isPinned = response.isPinned ?: return skipped(entity = "ClientNote", field = "isPinned")
        val createdAt = parseInstant(response.createdAt)
            ?: return skipped(entity = "ClientNote", field = "createdAt")
        val updatedAt = parseInstant(response.updatedAt)
            ?: return skipped(entity = "ClientNote", field = "updatedAt")
        return ClientNote(
            id = id,
            clientUserId = clientUserId,
            kind = kind,
            title = title,
            details = response.details,
            isPinned = isPinned,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    fun toCoachSummary(response: CoachSummaryResponse): CoachSummary? {
        val coachId = response.coachId ?: return skipped(entity = "CoachSummary", field = "coachId")
        val userId = response.userId ?: return skipped(entity = "CoachSummary", field = "userId")
        val displayName = response.displayName ?: return skipped(entity = "CoachSummary", field = "displayName")
        val zoneId = response.zoneId ?: return skipped(entity = "CoachSummary", field = "zoneId")
        val cancellationWindowHours = response.cancellationWindowHours
            ?: return skipped(entity = "CoachSummary", field = "cancellationWindowHours")
        return CoachSummary(
            coachId = coachId,
            userId = userId,
            displayName = displayName,
            zoneId = zoneId,
            cancellationWindowHours = cancellationWindowHours,
        )
    }

    fun toCoachClient(response: CoachClientResponse): CoachClient? {
        val userId = response.userId ?: return skipped(entity = "CoachClient", field = "userId")
        val displayName = response.displayName ?: return skipped(entity = "CoachClient", field = "displayName")
        return CoachClient(userId = userId, displayName = displayName)
    }

    private fun parseInstant(raw: String?): Instant? {
        if (raw == null) return null
        return runCatching { Instant.parse(raw) }.getOrNull()
    }

    private fun <T> skipped(entity: String, field: String): T? {
        logger.error(tag = LOG_TAG, message = "Пропущен $entity: в ответе нет или не разобрано поле $field")
        return null
    }
}
