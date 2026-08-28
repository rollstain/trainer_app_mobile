package app.trainer.feature.schedule.presentation.groupsession.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupSessionScreenModel
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupSessionSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

class GroupSessionScreen(private val slotId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: GroupSessionScreenModel = koinScreenModel(
            parameters = { parametersOf(slotId) },
        )
        val state by screenModel.collectAsState()

        GroupSessionView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is GroupSessionSideEffect.OpenClientCard ->
                    navigator.push(Screens.ClientCard(clientUserId = effect.clientUserId))
                GroupSessionSideEffect.SessionChanged -> Unit
                is GroupSessionSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
