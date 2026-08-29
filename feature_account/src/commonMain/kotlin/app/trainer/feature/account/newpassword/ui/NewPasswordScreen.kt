package app.trainer.feature.account.newpassword.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.newpassword.mvi.NewPasswordScreenModel
import app.trainer.feature.account.newpassword.mvi.NewPasswordSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class NewPasswordScreen(
    private val resetToken: String?,
    private val claimToken: String?,
) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: NewPasswordScreenModel = koinScreenModel(
            parameters = { parametersOf(resetToken, claimToken) },
        )
        val state by screenModel.collectAsState()

        NewPasswordView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            when (effect) {
                NewPasswordSideEffect.PasswordChanged -> Unit
                NewPasswordSideEffect.OpenRecovery -> navigator.pop()
                is NewPasswordSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
