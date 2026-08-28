package app.trainer.feature.progress.presentation.progress.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.progress.presentation.checkin.ui.CHECK_IN_SAVED
import app.trainer.feature.progress.presentation.progress.mvi.ProgressEvent
import app.trainer.feature.progress.presentation.progress.mvi.ProgressScreenModel
import app.trainer.feature.progress.presentation.progress.mvi.ProgressSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class ProgressScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: ProgressScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        navigator.handleResult(CHECK_IN_SAVED) {
            screenModel.dispatch(event = ProgressEvent.OnReloadRequested)
        }

        ProgressView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is ProgressSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
                is ProgressSideEffect.OpenCheckIn -> navigator.push(Screens.CheckIn(dateIso = effect.dateIso))
                ProgressSideEffect.OpenPhotoCompare -> navigator.push(Screens.PhotoCompare(clientUserId = null))
                ProgressSideEffect.OpenFormChecks -> navigator.push(Screens.FormChecks)
            }
        }
    }
}
