package app.trainer.feature.account.invite.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.account.invite.mvi.InviteScreenModel
import app.trainer.feature.account.invite.mvi.InviteSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class InviteScreen(private val code: String?) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: InviteScreenModel = koinScreenModel(
            parameters = { parametersOf(code.orEmpty()) },
        )
        val state by screenModel.collectAsState()

        InviteView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(
    effect: InviteSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        InviteSideEffect.OpenContactLink -> navigator.replaceAll(Screens.ContactLink)
        is InviteSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
    }
}
