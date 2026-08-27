package app.trainer.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
fun NavContainer(
    entries: List<NavEntry<Any>>,
    onBack: () -> Unit,
    forward: ContentTransform,
    backward: ContentTransform,
) {
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = entries.size > 1,
        onBackCompleted = onBack,
    )
    val topEntry = entries.lastOrNull() ?: return
    var renderedDepth by remember { mutableIntStateOf(entries.size) }
    val isForward = entries.size >= renderedDepth
    SideEffect { renderedDepth = entries.size }
    AnimatedContent(
        targetState = topEntry,
        transitionSpec = { if (isForward) forward else backward },
        contentKey = { it.contentKey },
        label = "NavContainer",
        content = { entry -> entry.Content() },
    )
}
