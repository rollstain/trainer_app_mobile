package app.trainer.feature.clientcard.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.clientcard.presentation.mvi.ClientCardScreenModel
import app.trainer.feature.clientcard.presentation.mvi.ClientCardSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

private const val NOTE_ARCHIVED_MESSAGE = "Пометка убрана в архив"

class ClientCardScreen(private val clientUserId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ClientCardScreenModel = koinScreenModel(
            parameters = { parametersOf(clientUserId) },
        )
        val state by screenModel.collectAsState()

        ClientCardView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(effect: ClientCardSideEffect, toastHost: ToastHostState) {
    when (effect) {
        is ClientCardSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
        ClientCardSideEffect.ShowNoteArchived -> toastHost.show(NOTE_ARCHIVED_MESSAGE)
    }
}
