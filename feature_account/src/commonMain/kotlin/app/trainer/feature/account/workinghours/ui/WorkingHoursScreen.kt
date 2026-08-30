package app.trainer.feature.account.workinghours.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.workinghours.mvi.WorkingHoursScreenModel
import app.trainer.feature.account.workinghours.mvi.WorkingHoursSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.ScreenRequestKey
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

val WORKING_HOURS_SAVED = ScreenRequestKey<Unit>("workingHoursSaved")

class WorkingHoursScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: WorkingHoursScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        WorkingHoursView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                WorkingHoursSideEffect.Close -> navigator.pop()
                WorkingHoursSideEffect.Saved -> navigator.popWithResult(WORKING_HOURS_SAVED, Unit)
                is WorkingHoursSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
