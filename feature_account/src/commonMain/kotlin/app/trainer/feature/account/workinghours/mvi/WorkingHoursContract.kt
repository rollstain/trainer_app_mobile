package app.trainer.feature.account.workinghours.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DayOfWeek

enum class WorkingHourIssue { Incomplete, EndBeforeStart }

data class WorkingHourRow(
    val dayOfWeek: DayOfWeek,
    val label: String,
    val isWorking: Boolean,
    val opensText: String,
    val closesText: String,
    val isPrefilled: Boolean,
    val isChanged: Boolean,
    val issue: WorkingHourIssue?,
)

data class WorkingHoursState(
    val rows: ImmutableList<WorkingHourRow>,
    val changedDays: Int,
    val isScheduleAbsent: Boolean,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
    val isSaving: Boolean,
    val isSaveFailed: Boolean,
    val isLeaveDialogVisible: Boolean,
) {

    val isDirty: Boolean
        get() = changedDays > 0

    val hasIssues: Boolean
        get() = rows.any { it.isWorking && it.issue != null }

    val allDaysOff: Boolean
        get() = rows.isNotEmpty() && rows.none { it.isWorking }

    val canApplyToAll: Boolean
        get() {
            val working = rows.filter { it.isWorking }
            if (working.size < 2) return false
            return working.any { day ->
                day.opensText != working.first().opensText || day.closesText != working.first().closesText
            }
        }

    companion object {

        fun initial(): WorkingHoursState = WorkingHoursState(
            rows = persistentListOf(),
            changedDays = 0,
            isScheduleAbsent = false,
            isLoading = true,
            failure = null,
            isSaving = false,
            isSaveFailed = false,
            isLeaveDialogVisible = false,
        )
    }
}

sealed interface WorkingHoursEvent {

    data object OnReloadRequested : WorkingHoursEvent

    data class OnDayToggled(val dayOfWeek: DayOfWeek) : WorkingHoursEvent

    data class OnOpensChanged(val dayOfWeek: DayOfWeek, val text: String) : WorkingHoursEvent

    data class OnClosesChanged(val dayOfWeek: DayOfWeek, val text: String) : WorkingHoursEvent

    data object OnApplyToAllClicked : WorkingHoursEvent

    data object OnSaveClicked : WorkingHoursEvent

    data object OnBackRequested : WorkingHoursEvent

    data object OnLeaveDialogDismissed : WorkingHoursEvent

    data object OnLeaveConfirmed : WorkingHoursEvent
}

sealed interface WorkingHoursSideEffect {

    data object Close : WorkingHoursSideEffect

    data object Saved : WorkingHoursSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : WorkingHoursSideEffect
}
