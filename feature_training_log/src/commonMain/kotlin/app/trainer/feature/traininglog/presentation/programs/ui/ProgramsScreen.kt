package app.trainer.feature.traininglog.presentation.programs.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.traininglog.presentation.programs.mvi.ProgramsScreenModel
import app.trainer.feature.traininglog.presentation.programs.mvi.ProgramsSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class ProgramsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ProgramsScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        ProgramsView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is ProgramsSideEffect.OpenProgram -> navigator.push(
                    Screens.ProgramEditor(programId = effect.programId)
                )
                is ProgramsSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
