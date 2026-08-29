package app.trainer.feature.account.profile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.contact.ui.CONTACT_LINK_REQUEST
import app.trainer.feature.account.profile.mvi.ProfileEvent
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

        navigator.handleResult(CONTACT_LINK_REQUEST) {
            screenModel.dispatch(event = ProfileEvent.OnReloadRequested)
        }

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

private suspend fun handleSideEffect(
    effect: ProfileSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        ProfileSideEffect.OpenContactLink -> navigator.push(Screens.ContactLink)
        ProfileSideEffect.OpenCoachSetup -> navigator.push(Screens.CoachSetup)
        ProfileSideEffect.OpenLoginMethods -> navigator.push(Screens.LoginMethods)
        ProfileSideEffect.OpenDevices -> navigator.push(Screens.Devices)
        ProfileSideEffect.OpenExerciseLibrary -> navigator.push(Screens.ExerciseLibrary)
        ProfileSideEffect.OpenPrograms -> navigator.push(Screens.Programs)
        ProfileSideEffect.OpenCoaches -> navigator.push(Screens.Coaches)
        ProfileSideEffect.SignedOut -> Unit
        is ProfileSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
