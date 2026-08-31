package app.trainer.feature.chat.presentation.list.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.chat.presentation.list.mvi.ChatListScreenModel
import app.trainer.feature.chat.presentation.list.mvi.ChatListSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class ChatListScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ChatListScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        ChatListView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: ChatListSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        is ChatListSideEffect.OpenDialog -> navigator.push(Screens.Chat(dialogId = effect.dialogId))
        ChatListSideEffect.OpenPeople -> navigator.selectRoot(Screens.CoachPeople)
        is ChatListSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
