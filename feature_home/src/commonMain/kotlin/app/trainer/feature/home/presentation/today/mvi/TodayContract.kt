package app.trainer.feature.home.presentation.today.mvi

import app.trainer.data.schedule.SlotChangeKind
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class TodayRequestRow(
    val requestId: String,
    val timeLabel: String,
    val proposedTimeLabel: String?,
    val requestedByDisplayName: String?,
    val kind: SlotChangeKind,
)

data class TodaySessionRow(
    val slotId: String,
    val clientUserId: String,
    val timeLabel: String,
    val durationLabel: String,
    val clientDisplayName: String,
    val isNext: Boolean,
    val startsInLabel: String,
)

data class TodayDialogRow(
    val dialogId: String,
    val peerDisplayName: String,
    val preview: String,
    val unreadCount: Long,
)

sealed interface LapsedSince {

    data object Never : LapsedSince

    data class Days(val value: Int) : LapsedSince
}

data class TodayLapsedRow(
    val userId: String,
    val displayName: String,
    val since: LapsedSince,
)

data class TodayCheckInRow(
    val checkInId: String,
    val clientUserId: String,
    val displayName: String,
    val dateLabel: String,
)

sealed interface TodayTomorrow {

    data object None : TodayTomorrow

    data class Sessions(val summary: String) : TodayTomorrow
}

sealed interface TodayNextSession {

    data object NoneThisWeek : TodayNextSession

    data class Upcoming(
        val dayLabel: String,
        val timeLabel: String,
        val clientDisplayName: String,
        val startsInLabel: String,
    ) : TodayNextSession
}

sealed interface TodayFreeSlots {

    data object None : TodayFreeSlots

    data class Available(val summary: String) : TodayFreeSlots
}

data class TodayState(
    val dateLabel: String,
    val coachDisplayName: String,
    val requests: ImmutableList<TodayRequestRow>,
    val sessions: ImmutableList<TodaySessionRow>,
    val unread: ImmutableList<TodayDialogRow>,
    val moreUnreadCount: Int,
    val lapsed: ImmutableList<TodayLapsedRow>,
    val awaitingCheckIns: ImmutableList<TodayCheckInRow>,
    val tomorrow: TodayTomorrow,
    val nextSession: TodayNextSession,
    val freeSlots: TodayFreeSlots,
    val hasClients: Boolean,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    val isQuiet: Boolean
        get() = requests.isEmpty() &&
            sessions.isEmpty() &&
            unread.isEmpty() &&
            lapsed.isEmpty() &&
            awaitingCheckIns.isEmpty()

    companion object {

        fun initial(): TodayState = TodayState(
            dateLabel = "",
            coachDisplayName = "",
            requests = persistentListOf(),
            sessions = persistentListOf(),
            unread = persistentListOf(),
            moreUnreadCount = 0,
            lapsed = persistentListOf(),
            awaitingCheckIns = persistentListOf(),
            tomorrow = TodayTomorrow.None,
            nextSession = TodayNextSession.NoneThisWeek,
            freeSlots = TodayFreeSlots.None,
            hasClients = true,
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface TodayEvent {

    data object OnRetryClicked : TodayEvent

    data object OnProfileClicked : TodayEvent

    data object OnCalendarClicked : TodayEvent

    data object OnAllDialogsClicked : TodayEvent

    data object OnAddSlotClicked : TodayEvent

    data class OnRequestResolved(val requestId: String, val approve: Boolean) : TodayEvent

    data class OnSessionClicked(val clientUserId: String) : TodayEvent

    data class OnDialogClicked(val dialogId: String) : TodayEvent

    data class OnLapsedClicked(val userId: String) : TodayEvent

    data class OnCheckInClicked(val clientUserId: String) : TodayEvent
}

sealed interface TodaySideEffect {

    data object OpenProfile : TodaySideEffect

    data object OpenCalendar : TodaySideEffect

    data object OpenChats : TodaySideEffect

    data object OpenSlotCreation : TodaySideEffect

    data class OpenClientCard(val clientUserId: String) : TodaySideEffect

    data class OpenDialog(val dialogId: String) : TodaySideEffect

    data class OpenDiary(val clientUserId: String) : TodaySideEffect

    data class ShowFailure(val failure: RequestResult.Error) : TodaySideEffect
}
