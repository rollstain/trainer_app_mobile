package app.trainer.feature.traininglog.presentation.coach.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.monthGenitiveOf
import app.trainer.base.format.VolumeFormat
import app.trainer.base.input.WeightInput
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.TrainingLogEntry
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.data.traininglog.TrainingSet
import app.trainer.entities.RequestResult
import app.trainer.feature.traininglog.domain.DurationInput
import app.trainer.strings.Res
import app.trainer.strings.training_log_distance_meters
import app.trainer.strings.training_log_duration_minutes
import kotlin.time.Clock
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.getString

private const val PERIOD_DAYS = 28
private const val SETS_SEPARATOR = " · "
private const val REPETITIONS_SIGN = " × "

class CoachTrainingLogScreenModel(
    private val clientUserId: String,
    private val trainingLogRepository: TrainingLogRepository,
    private val weightInput: WeightInput,
    private val durationInput: DurationInput,
    private val volumeFormat: VolumeFormat,
) : BaseScreenModel<CoachTrainingLogState, CoachTrainingLogSideEffect, CoachTrainingLogEvent>(
    initialState = CoachTrainingLogState.initial(clientUserId = clientUserId),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            loadPeriod(from = today.minus(DatePeriod(days = PERIOD_DAYS)), to = today)
        }
    }

    override fun dispatch(event: CoachTrainingLogEvent) {
        when (event) {
            CoachTrainingLogEvent.OnRetryClicked -> onFetchData()
            CoachTrainingLogEvent.OnPreviousPeriodClicked -> shiftPeriod(days = -PERIOD_DAYS)
            CoachTrainingLogEvent.OnNextPeriodClicked -> shiftPeriod(days = PERIOD_DAYS)
        }
    }

    private fun shiftPeriod(days: Int) {
        screenModelScope { state ->
            val from = state.from ?: return@screenModelScope
            val to = state.to ?: return@screenModelScope
            loadPeriod(
                from = from.plus(DatePeriod(days = days)),
                to = to.plus(DatePeriod(days = days)),
            )
        }
    }

    private suspend fun loadPeriod(from: LocalDate, to: LocalDate) {
        updateState { it.copy(isLoading = true, failure = null) }
        val loaded = trainingLogRepository.clientEntries(
            clientUserId = clientUserId,
            from = from,
            to = to,
        )
        when (loaded) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = loaded) }
                postSideEffect(CoachTrainingLogSideEffect.ShowFailure(loaded))
            }
            is RequestResult.Success -> showEntries(from = from, to = to, entries = loaded.data)
        }
    }

    private suspend fun showEntries(from: LocalDate, to: LocalDate, entries: List<TrainingLogEntry>) {
        val totalVolume = entries.sumOf { it.totalVolumeGrams }
        val periodLabel = "${formatDate(from)} — ${formatDate(to)}"
        val dayRows = entries.map { toRow(it) }
        val totalVolumeLabel = volumeFormat.toTons(totalVolume)
        updateState { current ->
            current.copy(
                from = from,
                to = to,
                periodLabel = periodLabel,
                totalWorkoutsLabel = entries.size.toString(),
                totalVolumeLabel = totalVolumeLabel,
                days = dayRows.toImmutableList(),
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun toRow(entry: TrainingLogEntry): LoggedDayRow = LoggedDayRow(
        entryId = entry.id,
        dateLabel = formatDate(entry.entryDate),
        volumeLabel = volumeFormat.toKilograms(entry.totalVolumeGrams),
        notes = entry.notes,
        exercises = entry.sets
            .groupBy { it.exerciseName }
            .map { (exerciseName, sets) ->
                ExerciseSummaryRow(
                    exerciseName = exerciseName,
                    setsLabel = sets.map { formatSet(it) }.joinToString(separator = SETS_SEPARATOR),
                )
            }
            .toImmutableList(),
    )

    private suspend fun formatSet(set: TrainingSet): String = when (set.kind) {
        ExerciseKind.STRENGTH -> {
            val repetitions = set.repetitions
            val weightGrams = set.weightGrams
            if (repetitions == null || weightGrams == null) {
                ""
            } else {
                "$repetitions$REPETITIONS_SIGN${weightInput.toKilogramsText(weightGrams)}"
            }
        }
        ExerciseKind.BODYWEIGHT -> set.repetitions?.toString().orEmpty()
        ExerciseKind.CARDIO -> formatCardio(set)
    }

    private suspend fun formatCardio(set: TrainingSet): String {
        val minutes = set.durationSeconds?.let(durationInput::toMinutesText)
        val distance = set.distanceMeters
        return listOfNotNull(
            minutes?.let { getString(Res.string.training_log_duration_minutes, it) },
            distance?.let { getString(Res.string.training_log_distance_meters, it) },
        ).joinToString(separator = " ")
    }

    private suspend fun formatDate(date: LocalDate): String {
        val month = monthGenitiveOf(date)
        return "${date.day} $month"
    }
}
