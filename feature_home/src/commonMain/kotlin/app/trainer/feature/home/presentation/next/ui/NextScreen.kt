package app.trainer.feature.home.presentation.next.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.home.presentation.next.mvi.NextScreenModel
import app.trainer.feature.home.presentation.next.mvi.NextSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class NextScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: NextScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        NextView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: NextSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        NextSideEffect.OpenProfile -> navigator.push(Screens.Profile)
        NextSideEffect.OpenBooking -> navigator.replaceAll(
            Screens.ClientBooking(coachId = null, weekStartIso = null)
        )
        NextSideEffect.OpenChat -> navigator.replaceAll(Screens.CoachChats)
        NextSideEffect.OpenInvite -> navigator.push(Screens.Invite(code = null))
        is NextSideEffect.OpenDiary -> navigator.push(Screens.ClientDiaryDay(dateIso = effect.dateIso))
        is NextSideEffect.OpenCheckIn -> navigator.push(Screens.CheckIn(dateIso = effect.dateIso))
        is NextSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
