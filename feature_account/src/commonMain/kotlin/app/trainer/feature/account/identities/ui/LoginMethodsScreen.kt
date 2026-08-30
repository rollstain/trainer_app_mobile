package app.trainer.feature.account.identities.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.identities.mvi.LoginMethodsScreenModel
import app.trainer.feature.account.identities.mvi.LoginMethodsSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.login_methods_email_sent
import app.trainer.strings.login_methods_linked_message
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString

class LoginMethodsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val uriHandler = LocalUriHandler.current
        val screenModel: LoginMethodsScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        LoginMethodsView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is LoginMethodsSideEffect.OpenTelegram -> uriHandler.openUri(effect.deepLink)
                LoginMethodsSideEffect.OpenPasswordForm -> navigator.push(Screens.PasswordForm)
                LoginMethodsSideEffect.OpenContactForm -> navigator.push(Screens.ContactLink)
                LoginMethodsSideEffect.ShowLinked ->
                    toastHost.show(getString(Res.string.login_methods_linked_message))
                LoginMethodsSideEffect.ShowConfirmationSent ->
                    toastHost.show(getString(Res.string.login_methods_email_sent))
                is LoginMethodsSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
