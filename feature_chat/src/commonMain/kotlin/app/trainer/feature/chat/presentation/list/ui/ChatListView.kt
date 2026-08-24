package app.trainer.feature.chat.presentation.list.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.feature.chat.presentation.list.mvi.ChatListEvent
import app.trainer.feature.chat.presentation.list.mvi.ChatListState
import app.trainer.feature.chat.presentation.list.mvi.DialogRow
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppCellShimmer
import app.trainer.uikit.widgets.AppListCell
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ListCellPreview
import app.trainer.uikit.widgets.ListCellTrailing
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import kotlinx.collections.immutable.ImmutableList

private const val SHIMMER_ROWS = 6
private const val TITLE = "Диалоги"
private const val EMPTY_TITLE = "Пока никого нет"
private const val EMPTY_DESCRIPTION =
    "Пригласите подопечного — он войдёт по коду, и здесь появится диалог."
private const val EMPTY_ACTION = "Создать приглашение"
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION =
    "Проверьте соединение. Отправленные сообщения останутся в очереди и уйдут сами."
private const val FAILURE_ACTION = "Повторить"

@Composable
fun ChatListView(
    modifier: Modifier = Modifier,
    state: ChatListState,
    onEvent: (ChatListEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = TITLE)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> LoadingList()
                state.isFailed -> AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(ChatListEvent.OnRetryClicked) },
                    ),
                )
                state.dialogs.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EMPTY_TITLE,
                    description = EMPTY_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = EMPTY_ACTION,
                        onClick = { onEvent(ChatListEvent.OnCreateInviteClicked) },
                    ),
                )
                else -> DialogList(dialogs = state.dialogs, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun DialogList(dialogs: ImmutableList<DialogRow>, onEvent: (ChatListEvent) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = dialogs, key = { it.dialogId }) { dialog ->
            AppListCell(
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
    }
}

@Composable
private fun LoadingList() {
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(SHIMMER_ROWS) { index ->
            AppCellShimmer(isLastRow = index == SHIMMER_ROWS - 1)
        }
    }
}
