package app.trainer.feature.traininglog.presentation.coach.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.traininglog.presentation.coach.mvi.CoachTrainingLogScreenModel
import app.trainer.feature.traininglog.presentation.coach.mvi.CoachTrainingLogSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class CoachTrainingLogScreen(private val clientUserId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: CoachTrainingLogScreenModel = koinScreenModel(
            parameters = { parametersOf(clientUserId) },
        )
        val state by screenModel.collectAsState()

        CoachTrainingLogView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(effect: CoachTrainingLogSideEffect, toastHost: ToastHostState) {
    when (effect) {
        is CoachTrainingLogSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
    }
}
