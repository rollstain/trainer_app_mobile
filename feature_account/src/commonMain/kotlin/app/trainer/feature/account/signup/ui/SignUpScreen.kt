package app.trainer.feature.account.signup.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.signup.mvi.SignUpScreenModel
import app.trainer.feature.account.signup.mvi.SignUpSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.compose.koinInject

class SignUpScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: SignUpScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        SignUpView(
            state = state,
            legalLinks = koinInject(),
            onEvent = { screenModel.dispatch(event = it) },
            onBack = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                SignUpSideEffect.SignedUp -> Unit
                is SignUpSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
