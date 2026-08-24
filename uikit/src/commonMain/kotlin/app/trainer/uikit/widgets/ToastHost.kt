package app.trainer.uikit.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.uikit.AppTheme
import kotlinx.coroutines.delay

private const val TOAST_VISIBLE_MILLIS = 4_000L
private const val DEFAULT_ACTION_TEXT = "Понятно"

@Stable
class ToastHostState {

    internal var message by mutableStateOf<String?>(null)
        private set

    fun show(text: String) {
        message = text
    }

    internal fun dismiss() {
        message = null
    }
}

val LocalToastHost = staticCompositionLocalOf<ToastHostState> {
    error("ToastHostState не предоставлен: оберните содержимое в AppToastHost")
}

@Composable
fun AppToastHost(
    modifier: Modifier = Modifier,
    state: ToastHostState = remember { ToastHostState() },
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalToastHost provides state) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            val message = state.message
            LaunchedEffect(message) {
                if (message == null) return@LaunchedEffect
                delay(TOAST_VISIBLE_MILLIS)
                state.dismiss()
            }
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AppTheme.spacing.dp16),
                visible = message != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                AppToast(
                    text = message.orEmpty(),
                    actionText = DEFAULT_ACTION_TEXT,
                    onAction = state::dismiss,
                )
            }
        }
    }
}
