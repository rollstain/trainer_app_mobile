package app.trainer.feature.account.invite.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.account.invite.mvi.InviteEvent
import app.trainer.feature.account.invite.mvi.InviteScreenModel
import app.trainer.feature.account.invite.mvi.InviteSideEffect
import app.trainer.navigation.Screen
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class InviteScreen(private val code: String?) : Screen {

    @Composable
    override fun Content() {
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: InviteScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        LaunchedEffect(Unit) {
            if (!code.isNullOrEmpty()) screenModel.dispatch(InviteEvent.OnCodeChanged(code))
        }

        InviteView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: InviteSideEffect,
    toastHost: ToastHostState,
) {
    when (effect) {
        InviteSideEffect.OpenContactLink -> Unit
        is InviteSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
