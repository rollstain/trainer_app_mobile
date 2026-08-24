package app.trainer.feature.clientcard.presentation.people.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleScreenModel
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleSideEffect
import app.trainer.navigation.DiaryPeriod
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class DiariesScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: PeopleScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        PeopleView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private fun handleSideEffect(
    effect: PeopleSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        is PeopleSideEffect.OpenPerson -> navigator.push(
            Screens.CoachClientDiary(clientUserId = effect.userId, period = DiaryPeriod.Month)
        )
        is PeopleSideEffect.ShowInviteCode -> toastHost.show("Код приглашения: ${effect.code}")
        is PeopleSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
    }
}
