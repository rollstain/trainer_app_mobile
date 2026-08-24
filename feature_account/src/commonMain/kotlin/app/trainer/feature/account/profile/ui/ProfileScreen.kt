package app.trainer.feature.account.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.account.profile.mvi.ProfileScreenModel
import app.trainer.feature.account.profile.mvi.ProfileSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class ProfileScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ProfileScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        ProfileView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(
    effect: ProfileSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        ProfileSideEffect.OpenContactLink -> navigator.push(Screens.ContactLink)
        ProfileSideEffect.SignedOut -> navigator.replaceAll(Screens.Invite(code = null))
        is ProfileSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
    }
}
