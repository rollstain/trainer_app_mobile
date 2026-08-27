package app.trainer.feature.traininglog.presentation.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorScreenModel
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorSideEffect
import app.trainer.navigation.Screen
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.training_log_editor_queued_message
import app.trainer.strings.training_log_editor_saved_message
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString
import org.koin.core.parameter.parametersOf

class TrainingLogEditorScreen(private val entryDateIso: String) : Screen {

    @Composable
    override fun Content() {
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: TrainingLogEditorScreenModel = koinScreenModel(
            parameters = { parametersOf(entryDateIso) },
        )
        val state by screenModel.collectAsState()

        TrainingLogEditorView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
        )

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(effect: TrainingLogEditorSideEffect, toastHost: ToastHostState) {
    when (effect) {
        is TrainingLogEditorSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
        TrainingLogEditorSideEffect.ShowSaved -> toastHost.show(getString(Res.string.training_log_editor_saved_message))
        TrainingLogEditorSideEffect.ShowQueued ->
            toastHost.show(getString(Res.string.training_log_editor_queued_message))
    }
}
