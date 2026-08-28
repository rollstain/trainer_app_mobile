package app.trainer.feature.account.devices.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DeviceRow(
    val sessionId: String,
    val deviceInfo: String,
    val lastSeenLabel: String,
    val isLongUnused: Boolean,
    val isCurrent: Boolean,
)

data class DevicesState(
    val devices: ImmutableList<DeviceRow>,
    val revokingSessionId: String?,
    val isRevokingOthers: Boolean,
    val isRevokeOthersDialogVisible: Boolean,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val hasOtherDevices: Boolean
        get() = devices.count { !it.isCurrent } > 0

    companion object {

        fun initial(): DevicesState = DevicesState(
            devices = persistentListOf(),
            revokingSessionId = null,
            isRevokingOthers = false,
            isRevokeOthersDialogVisible = false,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface DevicesEvent {

    data object OnReloadRequested : DevicesEvent

    data class OnDeviceRevoked(val sessionId: String) : DevicesEvent

    data object OnRevokeOthersClicked : DevicesEvent

    data object OnRevokeOthersConfirmed : DevicesEvent

    data object OnRevokeOthersDismissed : DevicesEvent
}

sealed interface DevicesSideEffect {

    data object ShowDeviceRevoked : DevicesSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : DevicesSideEffect
}
