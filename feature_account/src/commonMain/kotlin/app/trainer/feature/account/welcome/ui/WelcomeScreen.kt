package app.trainer.feature.account.welcome.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.welcome.mvi.WelcomeScreenModel
import app.trainer.feature.account.welcome.mvi.WelcomeSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class WelcomeScreen(private val afterSessionExpiry: Boolean) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val uriHandler = LocalUriHandler.current
        val screenModel: WelcomeScreenModel = koinScreenModel(
            parameters = { parametersOf(afterSessionExpiry) },
        )
        val state by screenModel.collectAsState()

        WelcomeView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is WelcomeSideEffect.OpenTelegram -> uriHandler.openUri(effect.deepLink)
                WelcomeSideEffect.OpenCodeEntry ->
                    navigator.push(Screens.Invite(afterSessionExpiry = false))
                is WelcomeSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
