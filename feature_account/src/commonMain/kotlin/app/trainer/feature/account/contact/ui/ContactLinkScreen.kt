package app.trainer.feature.account.contact.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.contact.mvi.ContactLinkScreenModel
import app.trainer.feature.account.contact.mvi.ContactLinkSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.ScreenRequestKey
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

internal val CONTACT_LINK_REQUEST = ScreenRequestKey<Unit>("contactLink")

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

private suspend fun handleSideEffect(
    effect: ContactLinkSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        ContactLinkSideEffect.Dismissed -> navigator.pop()
        ContactLinkSideEffect.Saved -> navigator.popWithResult(CONTACT_LINK_REQUEST, Unit)
        is ContactLinkSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
