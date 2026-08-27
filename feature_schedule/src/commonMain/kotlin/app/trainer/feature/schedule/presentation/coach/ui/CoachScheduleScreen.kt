package app.trainer.feature.schedule.presentation.coach.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleEvent
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleScreenModel
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleSideEffect
import app.trainer.feature.schedule.presentation.newslot.ui.NEW_SLOT_REQUEST
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class CoachScheduleScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: CoachScheduleScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        navigator.handleResult(NEW_SLOT_REQUEST) {
            screenModel.dispatch(event = CoachScheduleEvent.OnSlotCreated)
        }

        CoachScheduleView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: CoachScheduleSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        is CoachScheduleSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
        is CoachScheduleSideEffect.OpenSlotCreation -> navigator.push(
            Screens.NewSlot(dateIso = effect.dateIso)
        )
    }
}
