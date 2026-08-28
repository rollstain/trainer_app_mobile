package app.trainer.feature.account.nocoach.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.nocoach.mvi.NoCoachScreenModel
import app.trainer.feature.account.nocoach.mvi.NoCoachSideEffect
import app.trainer.navigation.Screen
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class NoCoachScreen : Screen {

    @Composable
    override fun Content() {
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: NoCoachScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        NoCoachView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            when (effect) {
                NoCoachSideEffect.Joined -> Unit
                is NoCoachSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
