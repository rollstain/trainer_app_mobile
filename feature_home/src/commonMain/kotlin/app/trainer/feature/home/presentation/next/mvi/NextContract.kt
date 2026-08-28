package app.trainer.feature.home.presentation.next.mvi

import app.trainer.entities.RequestResult
import app.trainer.uikit.widgets.HabitWeekDay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

sealed interface NextSessionCard {

    data object NoCoach : NextSessionCard

    data object NoSlots : NextSessionCard

    data class SlotsAvailable(val summary: String) : NextSessionCard

    data class Booked(
        val dayLabel: String,
        val timeLabel: String,
        val startsInLabel: String,
        val coachDisplayName: String,
        val isToday: Boolean,
        val canRequestChange: Boolean,
    ) : NextSessionCard
}

enum class FillKind { Diary, CheckIn }

sealed interface FillStatus {

    data object Pending : FillStatus

    data object DoneToday : FillStatus

    data object NeverFilled : FillStatus

    data class Overdue(val days: Int) : FillStatus
}

data class FillRow(
    val kind: FillKind,
    val title: String,
    val status: FillStatus,
)

data class PlannedExerciseRow(
    val exerciseId: String,
    val name: String,
    val details: String,
)

sealed interface PlannedToday {

    data object None : PlannedToday

    data class Workout(
        val programTitle: String,
        val dayTitle: String,
        val exercises: ImmutableList<PlannedExerciseRow>,
    ) : PlannedToday
}

data class NextHabitRow(
    val habitId: String,
    val title: String,
    val isSetByCoach: Boolean,
    val doneCountLabel: String,
    val days: ImmutableList<HabitWeekDay>,
)

sealed interface NextDynamics {

    data object NoCheckIns : NextDynamics

    data class Weight(
        val valueLabel: String,
        val dateLabel: String,
        val deltaLabel: String,
        val isWeightDown: Boolean,
        val values: ImmutableList<Float>,
        val maxLabel: String,
        val minLabel: String,
        val rangeLabel: String,
        val measuresLabel: String,
    ) : NextDynamics
}

enum class NextBlock { Session, Planned, Fills, Habits, Dynamics }

data class NextState(
    val clientDisplayName: String,
    val session: NextSessionCard,
    val planned: PlannedToday,
    val fills: ImmutableList<FillRow>,
    val habits: ImmutableList<NextHabitRow>,
    val weekdayLabels: ImmutableList<String>,
    val dynamics: NextDynamics,
    val failedBlocks: ImmutableSet<NextBlock>,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(): NextState = NextState(
            clientDisplayName = "",
            session = NextSessionCard.NoCoach,
            planned = PlannedToday.None,
            fills = persistentListOf(),
            habits = persistentListOf(),
            weekdayLabels = persistentListOf(),
            dynamics = NextDynamics.NoCheckIns,
            failedBlocks = persistentSetOf(),
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface NextEvent {

    data object OnRetryClicked : NextEvent

    data class OnBlockRetryClicked(val block: NextBlock) : NextEvent

    data object OnProfileClicked : NextEvent

    data object OnBookingClicked : NextEvent

    data object OnChatClicked : NextEvent

    data object OnInviteCodeClicked : NextEvent

    data class OnFillClicked(val kind: FillKind) : NextEvent
}

sealed interface NextSideEffect {

    data object OpenProfile : NextSideEffect

    data object OpenBooking : NextSideEffect

    data object OpenChat : NextSideEffect

    data object OpenInvite : NextSideEffect

    data class OpenDiary(val dateIso: String) : NextSideEffect

    data class OpenCheckIn(val dateIso: String) : NextSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : NextSideEffect
}
