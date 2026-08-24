package app.trainer.feature.progress.presentation.checkin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.progress.presentation.checkin.mvi.CheckInScreenModel
import app.trainer.feature.progress.presentation.checkin.mvi.CheckInSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

private const val SAVED_MESSAGE = "Чек-ин сохранён"

class CheckInScreen(private val dateIso: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: CheckInScreenModel = koinScreenModel(parameters = { parametersOf(dateIso) })
        val state by screenModel.collectAsState()

        CheckInView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is CheckInSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
                CheckInSideEffect.ShowSaved -> {
                    toastHost.show(SAVED_MESSAGE)
                    navigator.pop()
                }
            }
        }
    }
}
