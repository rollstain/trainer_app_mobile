package app.trainer.data.clients

import app.trainer.entities.Paged
import app.trainer.entities.RequestResult

interface ParticipantsRepository {

    suspend fun coachesOfClient(): RequestResult<List<CoachSummary>>

    suspend fun clientsOfCoach(
        limit: Int? = null,
        after: String? = null,
        query: String? = null,
    ): RequestResult<Paged<List<CoachClient>>>

    suspend fun clientsByIds(userIds: List<String>): RequestResult<List<CoachClient>>

    suspend fun missedSessions(clientUserIds: List<String>): RequestResult<Map<String, Int>>

    suspend fun archiveClient(clientUserId: String): RequestResult<Unit>

    suspend fun coachPolicy(): RequestResult<CoachPolicy>

    suspend fun updateCoachPolicy(policy: CoachPolicy): RequestResult<CoachPolicy>
}

interface ClientNotesRepository {

    suspend fun notesOf(clientUserId: String): RequestResult<List<ClientNote>>

    suspend fun pinnedNotes(): RequestResult<List<ClientNote>>

    suspend fun create(clientUserId: String, draft: ClientNoteDraft): RequestResult<ClientNote>

    suspend fun update(noteId: String, draft: ClientNoteDraft): RequestResult<ClientNote>

    suspend fun archive(noteId: String): RequestResult<Unit>
}
