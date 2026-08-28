package app.trainer.feature.account.devices.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.data.auth.DeviceSession
import app.trainer.data.auth.DeviceSessionsRepository
import app.trainer.entities.RequestResult
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val LONG_UNUSED_PERIOD = 90.days

class DevicesScreenModel(
    private val sessionsRepository: DeviceSessionsRepository,
) : BaseScreenModel<DevicesState, DevicesSideEffect, DevicesEvent>(
    initialState = DevicesState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = sessionsRepository.sessions()) {
                is RequestResult.Error -> updateState { it.copy(isLoading = false, failure = loaded) }
                is RequestResult.Success -> updateState {
                    it.copy(
                        isLoading = false,
                        devices = loaded.data.map(::rowOf).toImmutableList(),
                    )
                }
            }
        }
    }

    override fun dispatch(event: DevicesEvent) {
        when (event) {
            DevicesEvent.OnReloadRequested -> onFetchData()
            is DevicesEvent.OnDeviceRevoked -> revokeDevice(event.sessionId)
            DevicesEvent.OnRevokeOthersClicked -> updateState { it.copy(isRevokeOthersDialogVisible = true) }
            DevicesEvent.OnRevokeOthersDismissed -> updateState { it.copy(isRevokeOthersDialogVisible = false) }
            DevicesEvent.OnRevokeOthersConfirmed -> revokeOtherDevices()
        }
    }

    private fun revokeDevice(sessionId: String) {
        screenModelScope {
            updateState { it.copy(revokingSessionId = sessionId) }
            val revoked = sessionsRepository.revokeSession(sessionId)
            updateState { it.copy(revokingSessionId = null) }
            finish(revoked)
        }
    }

    private fun revokeOtherDevices() {
        screenModelScope {
            updateState { it.copy(isRevokeOthersDialogVisible = false, isRevokingOthers = true) }
            val revoked = sessionsRepository.revokeOtherSessions()
            updateState { it.copy(isRevokingOthers = false) }
            finish(revoked)
        }
    }

    private suspend fun finish(revoked: RequestResult<Unit>) {
        when (revoked) {
            is RequestResult.Error -> postSideEffect(DevicesSideEffect.ShowFailure(revoked))
            is RequestResult.Success -> {
                postSideEffect(DevicesSideEffect.ShowDeviceRevoked)
                onFetchData()
            }
        }
    }

    private fun rowOf(session: DeviceSession): DeviceRow {
        val lastSeenAt = Instant.parse(session.lastSeenAtIso)
        val lastSeenLocal = lastSeenAt.toLocalDateTime(TimeZone.currentSystemDefault())
        return DeviceRow(
            sessionId = session.id,
            deviceInfo = session.deviceInfo,
            lastSeenLabel = "${dayMonthOf(lastSeenLocal.date)} ${timeOfDayOf(lastSeenLocal)}",
            isLongUnused = lastSeenAt < Clock.System.now() - LONG_UNUSED_PERIOD,
            isCurrent = session.isCurrent,
        )
    }
}
