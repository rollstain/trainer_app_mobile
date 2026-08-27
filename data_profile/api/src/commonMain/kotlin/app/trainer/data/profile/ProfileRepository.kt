package app.trainer.data.profile

import app.trainer.entities.LocalDataCleaner
import app.trainer.entities.RequestResult

interface ProfileRepository : LocalDataCleaner {

    suspend fun me(): RequestResult<UserProfile>

    suspend fun lastKnownIsCoach(): Boolean?

    suspend fun updateContact(phone: String?, email: String?): RequestResult<UserProfile>
}
