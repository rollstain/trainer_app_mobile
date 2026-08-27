package app.trainer.feature.traininglog.presentation.programeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.traininglog.presentation.programday.ui.PROGRAM_DAY_SAVED
import app.trainer.feature.traininglog.presentation.programeditor.mvi.ProgramEditorEvent
import app.trainer.feature.traininglog.presentation.programeditor.mvi.ProgramEditorScreenModel
import app.trainer.feature.traininglog.presentation.programeditor.mvi.ProgramEditorSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class ProgramEditorScreen(private val programId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ProgramEditorScreenModel = koinScreenModel(
            parameters = { parametersOf(programId) },
        )
        val state by screenModel.collectAsState()

        navigator.handleResult(PROGRAM_DAY_SAVED) {
            screenModel.dispatch(event = ProgramEditorEvent.OnRetryClicked)
        }

        ProgramEditorView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is ProgramEditorSideEffect.OpenDay -> navigator.push(
                    Screens.ProgramDay(
                        programId = effect.programId,
                        weekNumber = effect.weekNumber,
                        dayOfWeek = effect.dayOfWeek,
                    )
                )
                is ProgramEditorSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
