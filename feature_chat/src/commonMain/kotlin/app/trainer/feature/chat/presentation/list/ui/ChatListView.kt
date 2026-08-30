package app.trainer.feature.chat.presentation.list.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.chat.presentation.list.mvi.ChatListEvent
import app.trainer.feature.chat.presentation.list.mvi.ChatListState
import app.trainer.strings.Res
import app.trainer.strings.chat_list_client_empty_description
import app.trainer.strings.chat_list_client_empty_title
import app.trainer.strings.chat_list_client_title
import app.trainer.strings.chat_list_coach_empty_action
import app.trainer.strings.chat_list_coach_empty_description
import app.trainer.strings.chat_list_coach_empty_title
import app.trainer.strings.chat_list_coach_title
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppCellShimmerList
import app.trainer.uikit.widgets.AppListCell
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ListCellPreview
import app.trainer.uikit.widgets.ListCellTrailing
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_ROWS = 6
private const val LOAD_MORE_ROWS = 2

@Composable
fun ChatListView(
    modifier: Modifier = Modifier,
    state: ChatListState,
    onEvent: (ChatListEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = if (state.isCoach) {
                stringResource(Res.string.chat_list_coach_title)
            } else {
                stringResource(Res.string.chat_list_client_title)
            },
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> AppCellShimmerList(count = SHIMMER_ROWS)
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ChatListEvent.OnRetryClicked) },
                )
                state.dialogs.isEmpty() && state.isCoach -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.chat_list_coach_empty_title),
                    description = stringResource(Res.string.chat_list_coach_empty_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.chat_list_coach_empty_action),
                        onClick = { onEvent(ChatListEvent.OnCreateInviteClicked) },
                    ),
                )
                state.dialogs.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.chat_list_client_empty_title),
                    description = stringResource(Res.string.chat_list_client_empty_description),
                )
                else -> DialogList(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun DialogList(state: ChatListState, onEvent: (ChatListEvent) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = state.dialogs, key = { it.dialogId }) { dialog ->
            AppListCell(
                modifier = Modifier.animateItem(),
                title = dialog.peerDisplayName,
                onClick = { onEvent(ChatListEvent.OnDialogClicked(dialog.dialogId)) },
                preview = dialog.lastMessagePreview
                    ?.let(ListCellPreview::Text)
                    ?: ListCellPreview.None,
                trailing = ListCellTrailing.TimeWithBadge(
                    time = dialog.lastMessageLabel,
                    unreadCount = dialog.unreadCount,
                ),
            )
        }
        if (state.hasMore) {
            item(key = "load-more") {
                LaunchedEffect(state.dialogs.size) { onEvent(ChatListEvent.OnEndReached) }
                AppCellShimmerList(count = LOAD_MORE_ROWS)
            }
        }
    }
}
