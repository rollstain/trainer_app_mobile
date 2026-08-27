package app.trainer.data.clients.impl

import app.trainer.data.clients.ClientNote
import app.trainer.data.clients.ClientNoteDraft
import app.trainer.data.clients.ClientNotesRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.safeRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ClientNotesRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: ClientsMapper,
) : ClientNotesRepository {

    private val client get() = httpClientProvider.client

    override suspend fun notesOf(clientUserId: String): RequestResult<List<ClientNote>> {
        val loaded = safeRequest<List<ClientNoteResponse>> {
            client.get("coach/clients/$clientUserId/notes")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toNote))
        }
    }

    override suspend fun pinnedNotes(): RequestResult<List<ClientNote>> {
        val loaded = safeRequest<List<ClientNoteResponse>> {
            client.get("coach/notes/pinned")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toNote))
        }
    }

    override suspend fun create(clientUserId: String, draft: ClientNoteDraft): RequestResult<ClientNote> {
        val created = safeRequest<ClientNoteResponse> {
            client.post("coach/clients/$clientUserId/notes") {
                contentType(ContentType.Application.Json)
                setBody(toRequest(draft))
            }
        }
        return noteOrError(created)
    }

    override suspend fun update(noteId: String, draft: ClientNoteDraft): RequestResult<ClientNote> {
        val updated = safeRequest<ClientNoteResponse> {
            client.put("coach/notes/$noteId") {
                contentType(ContentType.Application.Json)
                setBody(toRequest(draft))
            }
        }
        return noteOrError(updated)
    }

    override suspend fun archive(noteId: String): RequestResult<Unit> {
        return safeRequest<Unit> {
            client.delete("coach/notes/$noteId")
        }
    }

    private fun toRequest(draft: ClientNoteDraft): ClientNoteRequest = ClientNoteRequest(
        kind = draft.kind.name,
        title = draft.title,
        details = draft.details,
        isPinned = draft.isPinned,
    )

    private fun noteOrError(result: RequestResult<ClientNoteResponse>): RequestResult<ClientNote> {
        return when (result) {
            is RequestResult.Error -> result
            is RequestResult.Success -> {
                val note = mapper.toNote(result.data)
                if (note == null) {
                    RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "",
                        devMessage = "Ответ сервера не удалось разобрать в ClientNote",
                    )
                } else {
                    RequestResult.Success(note)
                }
            }
        }
    }
}
