package app.trainer.feature.account.telegramlink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.telegramlink.mvi.TelegramLinkScreenModel
import app.trainer.feature.account.telegramlink.mvi.TelegramLinkSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class TelegramLinkScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val uriHandler = LocalUriHandler.current
        val screenModel: TelegramLinkScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        TelegramLinkView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is TelegramLinkSideEffect.OpenTelegram -> uriHandler.openUri(effect.deepLink)
                TelegramLinkSideEffect.Done -> navigator.replaceAll(Screens.NoCoach)
                is TelegramLinkSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
