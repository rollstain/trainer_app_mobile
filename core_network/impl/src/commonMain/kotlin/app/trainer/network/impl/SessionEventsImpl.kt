package app.trainer.network.impl

import app.trainer.network.SessionEvents
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val SESSION_EVENTS_BUFFER = 1

class SessionEventsImpl : SessionEvents {

    private val mutableExpired = MutableSharedFlow<Unit>(
        extraBufferCapacity = SESSION_EVENTS_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val mutableAuthChanged = MutableSharedFlow<Unit>(
        extraBufferCapacity = SESSION_EVENTS_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val mutableProfileChanged = MutableSharedFlow<Unit>(
        extraBufferCapacity = SESSION_EVENTS_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val expired: Flow<Unit> = mutableExpired

    override val authChanged: Flow<Unit> = mutableAuthChanged

    override val profileChanged: Flow<Unit> = mutableProfileChanged

    override suspend fun notifyExpired() {
        mutableExpired.emit(Unit)
    }

    override suspend fun notifyAuthChanged() {
        mutableAuthChanged.emit(Unit)
    }

    override suspend fun notifyProfileChanged() {
        mutableProfileChanged.emit(Unit)
    }
}
