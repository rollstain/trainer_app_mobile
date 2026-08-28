package app.trainer.feature.account.invitelink.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.invitelink.mvi.InviteLinkScreenModel
import app.trainer.feature.account.invitelink.mvi.InviteLinkSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class InviteLinkScreen(private val code: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: InviteLinkScreenModel = koinScreenModel(
            parameters = { parametersOf(code) },
        )
        val state by screenModel.collectAsState()

        InviteLinkView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is InviteLinkSideEffect.OpenOnboarding -> navigator.push(Screens.Onboarding(code = effect.code))
                InviteLinkSideEffect.OpenCodeEntry -> navigator.push(Screens.Invite(afterSessionExpiry = false))
                InviteLinkSideEffect.SignedIn -> Unit
                is InviteLinkSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
