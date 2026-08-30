package app.trainer.feature.chat.presentation.list.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.timeOfDayOf
import app.trainer.data.chat.ChatRepository
import app.trainer.data.chat.Dialog
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestResult
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ChatListScreenModel(
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<ChatListState, ChatListSideEffect, ChatListEvent>(
    initialState = ChatListState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            if (!loadRole()) return@onFetchDataScope
            observeDialogs()
        }
        refresh(isPullToRefresh = false)
    }

    private suspend fun loadRole(): Boolean {
        profileRepository.lastKnownIsCoach()?.let { isCoach ->
            updateState { it.copy(isCoach = isCoach) }
            return true
        }
        return when (val profile = profileRepository.me()) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = profile) }
                postSideEffect(ChatListSideEffect.ShowFailure(profile))
                false
            }
            is RequestResult.Success -> {
                updateState { it.copy(isCoach = profile.data.coachId != null) }
                true
            }
        }
    }

    override fun dispatch(event: ChatListEvent) {
        when (event) {
            ChatListEvent.OnRetryClicked -> refresh(isPullToRefresh = false)
            ChatListEvent.OnRefreshRequested -> refresh(isPullToRefresh = true)
            ChatListEvent.OnEndReached -> loadMore()
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
                    failure = if (dialogs.isEmpty()) current.failure else null,
                )
            }
        }
    }

    private fun refresh(isPullToRefresh: Boolean) {
        screenModelScope {
            updateState { it.copy(isRefreshing = isPullToRefresh, failure = null) }
            when (val refreshed = chatRepository.refreshDialogs()) {
                is RequestResult.Error -> {
                    updateState { current ->
                        current.copy(
                            isRefreshing = false,
                            failure = refreshed.takeIf { current.dialogs.isEmpty() },
                        )
                    }
                    postSideEffect(ChatListSideEffect.ShowFailure(refreshed))
                }
                is RequestResult.Success -> {
                    updateState { it.copy(isRefreshing = false, failure = null, hasMore = refreshed.data) }
                }
            }
        }
    }

    private fun loadMore() {
        screenModelScope { state ->
            if (state.isLoadingMore || !state.hasMore) return@screenModelScope
            updateState { it.copy(isLoadingMore = true) }
            when (val loaded = chatRepository.loadMoreDialogs()) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingMore = false) }
                    postSideEffect(ChatListSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> updateState {
                    it.copy(isLoadingMore = false, hasMore = loaded.data)
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

    private fun formatTimeOfDay(instant: Instant): String =
        timeOfDayOf(instant.toLocalDateTime(TimeZone.currentSystemDefault()))
}
