package app.trainer.feature.clientcard.presentation.diaries.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.clientcard.presentation.diaries.mvi.DiariesScreenModel
import app.trainer.feature.clientcard.presentation.diaries.mvi.DiariesSideEffect
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
        val screenModel: DiariesScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        DiariesView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: DiariesSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        is DiariesSideEffect.OpenDiary -> navigator.push(
            Screens.CoachClientDiary(clientUserId = effect.userId, period = DiaryPeriod.Month)
        )
        is DiariesSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
