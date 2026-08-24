package app.trainer.data.profile

import app.trainer.entities.RequestResult

interface ProfileRepository {

    suspend fun me(): RequestResult<UserProfile>

    suspend fun updateContact(phone: String?, email: String?): RequestResult<UserProfile>
}
