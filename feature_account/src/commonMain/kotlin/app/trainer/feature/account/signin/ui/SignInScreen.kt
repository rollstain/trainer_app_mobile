package app.trainer.feature.account.signin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.signin.mvi.SignInScreenModel
import app.trainer.feature.account.signin.mvi.SignInSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class SignInScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val uriHandler = LocalUriHandler.current
        val screenModel: SignInScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        SignInView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBack = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                SignInSideEffect.SignedIn -> Unit
                is SignInSideEffect.OpenRecovery -> navigator.push(Screens.PasswordRecovery(email = effect.email))
                is SignInSideEffect.OpenTelegram -> uriHandler.openUri(effect.deepLink)
                is SignInSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
