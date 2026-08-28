package app.trainer.feature.account.devices.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.devices.mvi.DevicesScreenModel
import app.trainer.feature.account.devices.mvi.DevicesSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.devices_revoked_message
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString

class DevicesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: DevicesScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        DevicesView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is DevicesSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
                DevicesSideEffect.ShowDeviceRevoked ->
                    toastHost.show(getString(Res.string.devices_revoked_message))
            }
        }
    }
}
