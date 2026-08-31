package app.trainer.feature.schedule.presentation.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleScreenModel
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.client_schedule_booked_message
import app.trainer.strings.client_schedule_request_sent_message
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString

class ClientScheduleScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ClientScheduleScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        ClientScheduleView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: ClientScheduleSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        is ClientScheduleSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
        ClientScheduleSideEffect.ShowSlotBooked -> toastHost.show(getString(Res.string.client_schedule_booked_message))
        ClientScheduleSideEffect.ShowChangeRequestSent ->
            toastHost.show(getString(Res.string.client_schedule_request_sent_message))
        ClientScheduleSideEffect.OpenChat -> navigator.selectRoot(Screens.CoachChats)
    }
}
