package app.trainer.feature.chat.presentation.list.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DialogRow(
    val dialogId: String,
    val peerDisplayName: String,
    val lastMessagePreview: String?,
    val lastMessageLabel: String,
    val unreadCount: Long,
)

data class ChatListState(
    val dialogs: ImmutableList<DialogRow>,
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isFailed: Boolean,
) {

    companion object {

        fun initial(): ChatListState = ChatListState(
            dialogs = persistentListOf(),
            isLoading = true,
            isRefreshing = false,
            isFailed = false,
        )
    }
}

sealed interface ChatListEvent {

    data object OnRetryClicked : ChatListEvent

    data object OnRefreshRequested : ChatListEvent

    data object OnCreateInviteClicked : ChatListEvent

    data class OnDialogClicked(val dialogId: String) : ChatListEvent
}

sealed interface ChatListSideEffect {

    data class OpenDialog(val dialogId: String) : ChatListSideEffect

    data object OpenPeople : ChatListSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ChatListSideEffect
}
