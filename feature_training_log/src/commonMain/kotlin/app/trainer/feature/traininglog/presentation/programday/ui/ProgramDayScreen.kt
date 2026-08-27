package app.trainer.feature.traininglog.presentation.programday.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.traininglog.presentation.programday.mvi.ProgramDayScreenModel
import app.trainer.feature.traininglog.presentation.programday.mvi.ProgramDaySideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.ScreenRequestKey
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

val PROGRAM_DAY_SAVED = ScreenRequestKey<Unit>("programDaySaved")

class ProgramDayScreen(
    private val programId: String,
    private val weekNumber: Int,
    private val dayOfWeek: Int,
) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ProgramDayScreenModel = koinScreenModel(
            parameters = { parametersOf(programId, weekNumber, dayOfWeek) },
        )
        val state by screenModel.collectAsState()

        ProgramDayView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                ProgramDaySideEffect.Saved -> navigator.popWithResult(PROGRAM_DAY_SAVED, Unit)
                is ProgramDaySideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
