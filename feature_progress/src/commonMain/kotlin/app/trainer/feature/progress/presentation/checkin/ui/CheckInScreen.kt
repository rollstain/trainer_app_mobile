package app.trainer.feature.progress.presentation.checkin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.progress.presentation.checkin.mvi.CheckInScreenModel
import app.trainer.feature.progress.presentation.checkin.mvi.CheckInSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.ScreenRequestKey
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.check_in_saved_message
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString
import org.koin.core.parameter.parametersOf

internal val CHECK_IN_SAVED = ScreenRequestKey<Unit>("checkInSaved")

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
                is CheckInSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
                CheckInSideEffect.ShowSaved -> {
                    toastHost.show(getString(Res.string.check_in_saved_message))
                    navigator.popWithResult(CHECK_IN_SAVED, Unit)
                }
            }
        }
    }
}
