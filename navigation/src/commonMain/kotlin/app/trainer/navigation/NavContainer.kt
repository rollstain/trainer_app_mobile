package app.trainer.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
fun NavContainer(entries: List<NavEntry<Any>>, onBack: () -> Unit) {
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = entries.size > 1,
        onBackCompleted = onBack,
    )
    val topEntry = entries.lastOrNull() ?: return
    AnimatedContent(
        targetState = topEntry,
        transitionSpec = { fadeIn(snap()) togetherWith fadeOut(snap()) },
        contentKey = { it.contentKey },
        label = "NavContainer",
        content = { entry -> entry.Content() },
    )
}
