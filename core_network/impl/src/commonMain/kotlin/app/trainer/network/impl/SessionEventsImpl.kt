package app.trainer.network.impl

import app.trainer.network.SessionEvents
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val EXPIRED_EVENTS_BUFFER = 1

class SessionEventsImpl : SessionEvents {

    private val mutableExpired = MutableSharedFlow<Unit>(
        extraBufferCapacity = EXPIRED_EVENTS_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val expired: Flow<Unit> = mutableExpired

    override suspend fun notifyExpired() {
        mutableExpired.emit(Unit)
    }
}
