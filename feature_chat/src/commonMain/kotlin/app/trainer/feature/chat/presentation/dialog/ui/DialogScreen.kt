package app.trainer.feature.chat.presentation.dialog.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import app.trainer.base.failure.toastMessage
import app.trainer.feature.chat.presentation.dialog.mvi.DialogScreenModel
import app.trainer.feature.chat.presentation.dialog.mvi.DialogSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import org.koin.core.parameter.parametersOf

private const val LATEST_MESSAGE_INDEX = 0

class DialogScreen(private val dialogId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost = LocalToastHost.current
        val uriHandler = LocalUriHandler.current
        val listState = rememberLazyListState()
        val screenModel: DialogScreenModel = koinScreenModel(
            parameters = { parametersOf(dialogId) },
        )
        val state by screenModel.collectAsState()

        DialogView(
            state = state,
            listState = listState,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is DialogSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
                is DialogSideEffect.OpenAttachment -> uriHandler.openUri(effect.downloadUrl)
                DialogSideEffect.ScrollToLatest -> listState.animateScrollToItem(LATEST_MESSAGE_INDEX)
            }
        }
    }
}
