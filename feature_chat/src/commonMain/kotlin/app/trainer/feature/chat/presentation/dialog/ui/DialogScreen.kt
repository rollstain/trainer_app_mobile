package app.trainer.feature.chat.presentation.dialog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.chat.presentation.dialog.mvi.DialogScreenModel
import app.trainer.feature.chat.presentation.dialog.mvi.DialogSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class DialogScreen(private val dialogId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: DialogScreenModel = koinScreenModel(
            parameters = { parametersOf(dialogId) },
        )
        val state by screenModel.collectAsState()

        DialogView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(effect: DialogSideEffect, toastHost: ToastHostState) {
    when (effect) {
        is DialogSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
        is DialogSideEffect.OpenAttachment -> Unit
        DialogSideEffect.ScrollToLatest -> Unit
    }
}
