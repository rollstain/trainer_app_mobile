package app.trainer.feature.traininglog.presentation.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorScreenModel
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

private const val SAVED_MESSAGE = "День сохранён"

class TrainingLogEditorScreen(private val entryDateIso: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: TrainingLogEditorScreenModel = koinScreenModel(
            parameters = { parametersOf(entryDateIso) },
        )
        val state by screenModel.collectAsState()

        TrainingLogEditorView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(effect: TrainingLogEditorSideEffect, toastHost: ToastHostState) {
    when (effect) {
        is TrainingLogEditorSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
        TrainingLogEditorSideEffect.ShowSaved -> toastHost.show(SAVED_MESSAGE)
    }
}
