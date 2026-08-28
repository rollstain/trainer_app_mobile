package app.trainer.feature.progress.presentation.formcheck.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachFormChecksScreenModel
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachFormChecksSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.form_checks_replied
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString

class CoachFormChecksScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: CoachFormChecksScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        CoachFormChecksView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                CoachFormChecksSideEffect.ShowReplied -> toastHost.show(getString(Res.string.form_checks_replied))
                is CoachFormChecksSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
