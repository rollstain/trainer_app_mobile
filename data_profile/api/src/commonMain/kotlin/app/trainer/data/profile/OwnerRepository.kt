package app.trainer.data.profile

import app.trainer.entities.Paged
import app.trainer.entities.RequestResult

interface OwnerRepository {

    suspend fun coaches(limit: Int? = null, after: String? = null): RequestResult<Paged<List<CoachAccount>>>

    suspend fun coach(coachId: String): RequestResult<CoachAccountCard>
}
