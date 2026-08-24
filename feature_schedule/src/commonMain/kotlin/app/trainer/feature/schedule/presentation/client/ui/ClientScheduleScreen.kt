package app.trainer.feature.schedule.presentation.client.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleScreenModel
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleSideEffect
import app.trainer.navigation.Screen
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

private const val BOOKED_MESSAGE = "Вы записаны на тренировку"
private const val REQUEST_SENT_MESSAGE = "Заявка отправлена, ждём ответа тренера"

class ClientScheduleScreen : Screen {

    @Composable
    override fun Content() {
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ClientScheduleScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        ClientScheduleView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(effect: ClientScheduleSideEffect, toastHost: ToastHostState) {
    when (effect) {
        is ClientScheduleSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
        ClientScheduleSideEffect.ShowSlotBooked -> toastHost.show(BOOKED_MESSAGE)
        ClientScheduleSideEffect.ShowChangeRequestSent -> toastHost.show(REQUEST_SENT_MESSAGE)
    }
}
