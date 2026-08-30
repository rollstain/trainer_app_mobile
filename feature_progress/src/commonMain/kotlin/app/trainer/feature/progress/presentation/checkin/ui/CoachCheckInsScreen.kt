package app.trainer.feature.progress.presentation.checkin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.progress.presentation.checkin.mvi.CoachCheckInsScreenModel
import app.trainer.feature.progress.presentation.checkin.mvi.CoachCheckInsSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class CoachCheckInsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: CoachCheckInsScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        CoachCheckInsView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is CoachCheckInsSideEffect.OpenClientCard ->
                    navigator.push(Screens.ClientCard(effect.clientUserId))
                is CoachCheckInsSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
