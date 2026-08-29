package app.trainer.data.auth

import app.trainer.entities.RequestResult

data class CoachRequest(
    val id: String,
    val displayName: String,
    val createdAtIso: String,
)

interface CoachRequestsRepository {

    suspend fun pending(): RequestResult<List<CoachRequest>>

    suspend fun approve(requestId: String): RequestResult<Unit>

    suspend fun decline(requestId: String): RequestResult<Unit>
}
