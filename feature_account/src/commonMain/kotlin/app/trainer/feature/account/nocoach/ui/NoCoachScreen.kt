package app.trainer.feature.account.nocoach.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.nocoach.mvi.NoCoachScreenModel
import app.trainer.feature.account.nocoach.mvi.NoCoachSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class NoCoachScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: NoCoachScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        NoCoachView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            when (effect) {
                NoCoachSideEffect.Joined -> Unit
                NoCoachSideEffect.OpenCoachSetup -> navigator.push(Screens.CoachSetup)
                is NoCoachSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
