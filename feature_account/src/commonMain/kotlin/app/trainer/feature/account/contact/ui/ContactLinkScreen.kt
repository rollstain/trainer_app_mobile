package app.trainer.feature.account.contact.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.account.contact.mvi.ContactLinkScreenModel
import app.trainer.feature.account.contact.mvi.ContactLinkSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class ContactLinkScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ContactLinkScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        ContactLinkView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(
    effect: ContactLinkSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        ContactLinkSideEffect.Finish -> navigator.replaceAll(
            Screens.ClientBooking(coachId = null, weekStartIso = null)
        )
        is ContactLinkSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
    }
}
