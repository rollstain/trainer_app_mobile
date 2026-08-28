package app.trainer.feature.account.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.onboarding.mvi.OnboardingScreenModel
import app.trainer.feature.account.onboarding.mvi.OnboardingSideEffect
import app.trainer.navigation.Screen
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class OnboardingScreen(private val code: String) : Screen {

    @Composable
    override fun Content() {
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: OnboardingScreenModel = koinScreenModel(
            parameters = { parametersOf(code) },
        )
        val state by screenModel.collectAsState()

        OnboardingView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            when (effect) {
                OnboardingSideEffect.SignedIn -> Unit
                is OnboardingSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
