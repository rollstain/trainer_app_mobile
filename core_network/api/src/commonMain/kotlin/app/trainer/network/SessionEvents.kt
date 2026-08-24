package app.trainer.network

import kotlinx.coroutines.flow.Flow

interface SessionEvents {

    val expired: Flow<Unit>

    suspend fun notifyExpired()
}
