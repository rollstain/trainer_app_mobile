package app.trainer.feature.progress.presentation.formcheck.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.progress.presentation.formcheck.mvi.FormChecksEvent
import app.trainer.feature.progress.presentation.formcheck.mvi.FormChecksScreenModel
import app.trainer.feature.progress.presentation.formcheck.mvi.FormChecksSideEffect
import app.trainer.media.rememberVideoPicker
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.form_checks_sent
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString

class FormChecksScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: FormChecksScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        val picker = rememberVideoPicker { video ->
            screenModel.dispatch(event = FormChecksEvent.OnVideoPicked(video))
        }

        FormChecksView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onSendClick = picker::pick,
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                FormChecksSideEffect.ShowSent -> toastHost.show(getString(Res.string.form_checks_sent))
                is FormChecksSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}
