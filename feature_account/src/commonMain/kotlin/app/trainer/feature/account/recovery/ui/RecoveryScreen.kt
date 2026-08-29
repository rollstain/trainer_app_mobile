package app.trainer.feature.account.recovery.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.recovery.mvi.RecoveryScreenModel
import app.trainer.feature.account.recovery.mvi.RecoverySideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class RecoveryScreen(private val email: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val uriHandler = LocalUriHandler.current
        val screenModel: RecoveryScreenModel = koinScreenModel(parameters = { parametersOf(email) })
        val state by screenModel.collectAsState()

        RecoveryView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBack = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is RecoverySideEffect.OpenTelegram -> uriHandler.openUri(effect.deepLink)
                is RecoverySideEffect.OpenNewPassword -> navigator.push(
                    Screens.NewPassword(resetToken = null, claimToken = effect.claimToken),
                )
                RecoverySideEffect.OpenCodeEntry -> navigator.push(Screens.Invite(afterSessionExpiry = false))
                is RecoverySideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
