package app.trainer.feature.clientcard.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.clientcard.presentation.mvi.ClientCardScreenModel
import app.trainer.feature.clientcard.presentation.mvi.ClientCardSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.ScreenRequestKey
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.client_card_archived_message
import app.trainer.strings.client_card_note_archived_message
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString
import org.koin.core.parameter.parametersOf

val CLIENT_ARCHIVED_REQUEST = ScreenRequestKey<Unit>("clientArchived")

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
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: ClientCardSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        is ClientCardSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
        ClientCardSideEffect.ShowNoteArchived ->
            toastHost.show(getString(Res.string.client_card_note_archived_message))
        ClientCardSideEffect.ClientArchived -> {
            toastHost.show(getString(Res.string.client_card_archived_message))
            navigator.popWithResult(CLIENT_ARCHIVED_REQUEST, Unit)
        }
        is ClientCardSideEffect.OpenPhotoCompare ->
            navigator.push(Screens.PhotoCompare(clientUserId = effect.clientUserId))
    }
}
