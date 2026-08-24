package app.trainer.feature.chat.presentation.list.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.chat.ChatRepository
import app.trainer.data.chat.Dialog
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ChatListScreenModel(
    private val chatRepository: ChatRepository,
) : BaseScreenModel<ChatListState, ChatListSideEffect, ChatListEvent>(
    initialState = ChatListState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            observeDialogs()
        }
        refresh(isPullToRefresh = false)
    }

    override fun dispatch(event: ChatListEvent) {
        when (event) {
            ChatListEvent.OnRetryClicked -> refresh(isPullToRefresh = false)
            ChatListEvent.OnRefreshRequested -> refresh(isPullToRefresh = true)
            ChatListEvent.OnCreateInviteClicked -> openPeople()
            is ChatListEvent.OnDialogClicked -> openDialog(event.dialogId)
        }
    }

    private suspend fun observeDialogs() {
        chatRepository.observeDialogs().collectLatest { dialogs ->
            updateState { current ->
                current.copy(
                    dialogs = dialogs.map(::toRow).toImmutableList(),
                    isLoading = false,
                )
            }
        }
    }

    private fun refresh(isPullToRefresh: Boolean) {
        screenModelScope {
            updateState { it.copy(isRefreshing = isPullToRefresh, isFailed = false) }
            when (val refreshed = chatRepository.refreshDialogs()) {
                is RequestResult.Error -> {
                    updateState { current ->
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isFailed = current.dialogs.isEmpty(),
                        )
                    }
                    postSideEffect(ChatListSideEffect.ShowFailure(refreshed))
                }
                is RequestResult.Success -> {
                    updateState { it.copy(isLoading = false, isRefreshing = false, isFailed = false) }
                }
            }
        }
    }

    private fun openDialog(dialogId: String) {
        screenModelScope {
            postSideEffect(ChatListSideEffect.OpenDialog(dialogId = dialogId))
        }
    }

    private fun openPeople() {
        screenModelScope {
            postSideEffect(ChatListSideEffect.OpenPeople)
        }
    }

    private fun toRow(dialog: Dialog): DialogRow = DialogRow(
        dialogId = dialog.id,
        peerDisplayName = dialog.peerDisplayName,
        lastMessagePreview = dialog.lastMessagePreview,
        lastMessageLabel = dialog.lastMessageAt?.let(::formatTimeOfDay).orEmpty(),
        unreadCount = dialog.unreadCount,
    )

    private fun formatTimeOfDay(instant: Instant): String {
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hours = dateTime.hour.toString().padStart(length = 2, padChar = '0')
        val minutes = dateTime.minute.toString().padStart(length = 2, padChar = '0')
        return "$hours:$minutes"
    }
}
