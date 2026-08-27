package app.trainer.feature.clientcard.presentation.people.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleEvent
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleScreenModel
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleSideEffect
import app.trainer.feature.clientcard.presentation.ui.CLIENT_ARCHIVED_REQUEST
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.people_invite_code_message
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString

class PeopleScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: PeopleScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        navigator.handleResult(CLIENT_ARCHIVED_REQUEST) {
            screenModel.dispatch(event = PeopleEvent.OnRetryClicked)
        }

        PeopleView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: PeopleSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        is PeopleSideEffect.OpenPerson -> navigator.push(
            Screens.ClientCard(clientUserId = effect.userId)
        )
        is PeopleSideEffect.ShowInviteCode ->
            toastHost.show(getString(Res.string.people_invite_code_message, effect.code))
        is PeopleSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
