package app.trainer.feature.traininglog.presentation.coach.mvi

import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate

data class ExerciseSummaryRow(
    val exerciseName: String,
    val setsLabel: String,
)

data class LoggedDayRow(
    val entryId: String,
    val dateLabel: String,
    val volumeLabel: String,
    val notes: String?,
    val exercises: ImmutableList<ExerciseSummaryRow>,
)

data class CoachTrainingLogState(
    val clientUserId: String,
    val from: LocalDate?,
    val to: LocalDate?,
    val periodLabel: String,
    val totalWorkoutsLabel: String,
    val totalVolumeLabel: String,
    val days: ImmutableList<LoggedDayRow>,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
) {

    companion object {

        fun initial(clientUserId: String): CoachTrainingLogState = CoachTrainingLogState(
            clientUserId = clientUserId,
            from = null,
            to = null,
            periodLabel = "",
            totalWorkoutsLabel = "",
            totalVolumeLabel = "",
            days = persistentListOf(),
            isLoading = true,
            failure = null,
        )
    }
}

sealed interface CoachTrainingLogEvent {

    data object OnRetryClicked : CoachTrainingLogEvent

    data object OnPreviousPeriodClicked : CoachTrainingLogEvent

    data object OnNextPeriodClicked : CoachTrainingLogEvent
}

sealed interface CoachTrainingLogSideEffect {

    data class ShowFailure(val failure: RequestResult.Error) : CoachTrainingLogSideEffect
}
