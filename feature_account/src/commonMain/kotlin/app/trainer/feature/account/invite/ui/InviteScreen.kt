package app.trainer.feature.account.invite.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
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

class InviteScreen(private val afterSessionExpiry: Boolean) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: InviteScreenModel = koinScreenModel(
            parameters = { parametersOf(afterSessionExpiry) },
        )
        val state by screenModel.collectAsState()

        InviteView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is InviteSideEffect.OpenOnboarding -> navigator.push(Screens.Onboarding(code = effect.code))
                InviteSideEffect.SignedIn -> Unit
                is InviteSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
