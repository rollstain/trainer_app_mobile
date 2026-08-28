package app.trainer.feature.traininglog.presentation.editor.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.monthGenitiveOf
import app.trainer.base.format.VolumeFormat
import app.trainer.base.input.WeightInput
import app.trainer.data.program.PlannedWorkout
import app.trainer.data.program.ProgramExerciseLine
import app.trainer.data.program.ProgramRepository
import app.trainer.data.traininglog.Exercise
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.data.traininglog.LastPerformed
import app.trainer.data.traininglog.SaveOutcome
import app.trainer.data.traininglog.TrainingDayInput
import app.trainer.data.traininglog.TrainingInputRow
import app.trainer.data.traininglog.TrainingInputStore
import app.trainer.data.traininglog.TrainingLogDraft
import app.trainer.data.traininglog.TrainingLogEntry
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.data.traininglog.TrainingSet
import app.trainer.data.traininglog.TrainingSetDraft
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.feature.traininglog.domain.DurationInput
import app.trainer.feature.traininglog.domain.RestCountdown
import app.trainer.feature.traininglog.domain.RestTimer
import app.trainer.feature.traininglog.presentation.label
import app.trainer.strings.Res
import app.trainer.strings.training_log_planned_summary
import app.trainer.strings.training_log_set_values_invalid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

private const val INPUT_SAVE_DELAY_MS = 500L
private const val SECONDS_IN_MINUTE = 60
private const val SECONDS_PAD_LENGTH = 2
private const val TIME_SEPARATOR = ":"

@OptIn(ExperimentalUuidApi::class)
class TrainingLogEditorScreenModel(
    entryDateIso: String,
    private val trainingLogRepository: TrainingLogRepository,
    private val trainingInputStore: TrainingInputStore,
    private val weightInput: WeightInput,
    private val durationInput: DurationInput,
    private val volumeFormat: VolumeFormat,
    private val restTimer: RestTimer,
    private val programRepository: ProgramRepository,
) : BaseScreenModel<TrainingLogEditorState, TrainingLogEditorSideEffect, TrainingLogEditorEvent>(
    initialState = TrainingLogEditorState.initial(entryDate = LocalDate.parse(entryDateIso)),
) {

    private val entryDate: LocalDate get() = state.entryDate

    private var slotId: String? = null

    private var plannedWorkout: PlannedWorkout? = null

    private var syncedInput: TrainingDayInput? = null

    private var inputObserver: Job? = null

    init {
        onFetchData()
        observeRest()
    }

    private fun observeRest() {
        screenModelScope {
            restTimer.countdown.collect { countdown ->
                updateState { it.copy(rest = countdown?.let(::toRestState)) }
            }
        }
    }

    private fun toRestState(countdown: RestCountdown): RestState = RestState(
        label = formatRest(countdown.remainingSeconds),
        progress = countdown.remainingSeconds.toFloat() / countdown.totalSeconds,
    )

    private fun formatRest(seconds: Int): String {
        val minutes = seconds / SECONDS_IN_MINUTE
        val rest = (seconds % SECONDS_IN_MINUTE).toString().padStart(SECONDS_PAD_LENGTH, '0')
        return "$minutes$TIME_SEPARATOR$rest"
    }

    override fun onFetchData() {
        screenModelScope { observeQueue() }
        onFetchDataScope {
            trainingLogRepository.sendQueuedEntries()
            updateState { it.copy(isLoading = true, failure = null) }
            val exercises = trainingLogRepository.availableExercises()
            if (exercises is RequestResult.Error) {
                updateState { it.copy(isLoading = false, failure = exercises) }
                postSideEffect(TrainingLogEditorSideEffect.ShowFailure(exercises))
                return@onFetchDataScope
            }
            val entries = trainingLogRepository.ownEntries(from = entryDate, to = entryDate)
            if (entries is RequestResult.Error) {
                updateState { it.copy(isLoading = false, failure = entries) }
                postSideEffect(TrainingLogEditorSideEffect.ShowFailure(entries))
                return@onFetchDataScope
            }
            showLoaded(
                exercises = (exercises as RequestResult.Success).data.items,
                entry = (entries as RequestResult.Success).data.firstOrNull(),
            )
            loadPlan()
        }
    }

    private suspend fun loadPlan() {
        val planned = programRepository.plannedWorkouts(from = entryDate, to = entryDate)
        if (planned !is RequestResult.Success) return
        val workout = planned.data.firstOrNull()?.takeIf { it.exercises.isNotEmpty() }
        plannedWorkout = workout
        val forDay = when (workout) {
            null -> PlannedForDay.None
            else -> PlannedForDay.Workout(
                dayTitle = workout.dayTitle,
                summary = getString(
                    Res.string.training_log_planned_summary,
                    workout.exercises.size,
                    workout.exercises.sumOf { it.setsCount },
                ),
            )
        }
        updateState { it.copy(planned = forDay) }
    }

    private fun applyPlan() {
        screenModelScope { state ->
            val workout = plannedWorkout ?: return@screenModelScope
            val rows = workout.exercises.flatMap { line -> rowsOf(line = line, options = state.exercises) }
            if (rows.isEmpty()) return@screenModelScope
            updateState { current -> current.copy(sets = (current.sets + rows).toImmutableList()) }
        }
    }

    private fun rowsOf(line: ProgramExerciseLine, options: List<ExerciseOption>): List<SetRow> {
        val option = options.firstOrNull { it.exerciseId == line.exerciseId } ?: return emptyList()
        return (0 until line.setsCount).map {
            emptyRowFor(option).copy(
                repetitionsText = line.repetitions?.toString().orEmpty(),
                weightText = line.weightGrams?.let(weightInput::toKilogramsText).orEmpty(),
            )
        }
    }

    private suspend fun observeQueue() {
        trainingLogRepository.observeQueuedDates().collectLatest { dates ->
            updateState { it.copy(isQueued = entryDate in dates) }
        }
    }

    override fun dispatch(event: TrainingLogEditorEvent) {
        when (event) {
            TrainingLogEditorEvent.OnRetryClicked -> onFetchData()
            TrainingLogEditorEvent.OnPlanApplied -> applyPlan()
            TrainingLogEditorEvent.OnSaveClicked -> save()
            is TrainingLogEditorEvent.OnExerciseAdded -> addSet(event.exerciseId)
            is TrainingLogEditorEvent.OnSetRemoved -> removeSet(event.rowId)
            is TrainingLogEditorEvent.OnSetDuplicated -> duplicateSet(event.rowId)
            is TrainingLogEditorEvent.OnNotesChanged -> updateState { it.copy(notes = event.notes) }
            TrainingLogEditorEvent.OnRestExtended -> screenModelScope { restTimer.extend() }
            TrainingLogEditorEvent.OnRestSkipped -> restTimer.stop()
            is TrainingLogEditorEvent.OnRepetitionsChanged -> updateRow(event.rowId) {
                it.copy(repetitionsText = event.text.filter(Char::isDigit))
            }
            is TrainingLogEditorEvent.OnWeightChanged -> updateRow(event.rowId) {
                it.copy(weightText = event.text)
            }
            is TrainingLogEditorEvent.OnDurationChanged -> updateRow(event.rowId) {
                it.copy(durationText = event.text.filter(Char::isDigit))
            }
            is TrainingLogEditorEvent.OnDistanceChanged -> updateRow(event.rowId) {
                it.copy(distanceText = event.text.filter(Char::isDigit))
            }
        }
    }

    private suspend fun showLoaded(exercises: List<Exercise>, entry: TrainingLogEntry?) {
        slotId = entry?.slotId
        val options = exercises.map { exercise ->
            ExerciseOption(
                exerciseId = exercise.id,
                name = exercise.name,
                muscleGroup = exercise.primaryMuscle?.let { getString(it.label()) },
                kind = exercise.kind,
                lastResult = toHints(exercise.lastPerformed),
            )
        }
        val hintsByExercise = options.associate { it.exerciseId to it.lastResult }
        val syncedRows = entry?.sets.orEmpty()
            .map { set -> toRow(set = set, hints = hintsByExercise[set.exerciseId]) }
        val syncedNotes = entry?.notes.orEmpty()
        syncedInput = TrainingDayInput(notes = syncedNotes, rows = syncedRows.map(::toInputRow))

        val storedInput = trainingInputStore.load(entryDate)
        val restoredRows = storedInput?.rows
            ?.map { row -> toRow(input = row, hints = hintsByExercise[row.exerciseId]) }

        val dateLabel = formatDate(entryDate)
        val volumeLabel = volumeFormat.toKilograms(entry?.totalVolumeGrams ?: 0)
        updateState { current ->
            current.copy(
                dateLabel = dateLabel,
                volumeLabel = volumeLabel,
                exercises = options.toImmutableList(),
                sets = (restoredRows ?: syncedRows).toImmutableList(),
                notes = storedInput?.notes ?: syncedNotes,
                isLoading = false,
                failure = null,
            )
        }
        observeInput()
    }

    @OptIn(FlowPreview::class)
    private fun observeInput() {
        inputObserver?.cancel()
        inputObserver = screenModelScope {
            stateChanges
                .map(::toInput)
                .distinctUntilChanged()
                .debounce(INPUT_SAVE_DELAY_MS)
                .collect { input ->
                    if (input == syncedInput) {
                        trainingInputStore.clear(entryDate)
                    } else {
                        trainingInputStore.save(entryDate = entryDate, input = input)
                    }
                }
        }
    }

    private fun addSet(exerciseId: String) {
        screenModelScope { state ->
            val exercise = state.exercises.firstOrNull { it.exerciseId == exerciseId } ?: return@screenModelScope
            updateState { current ->
                current.copy(
                    sets = (current.sets + emptyRowFor(exercise)).toImmutableList()
                )
            }
        }
    }

    private fun duplicateSet(rowId: String) {
        screenModelScope { state ->
            val source = state.sets.firstOrNull { it.rowId == rowId } ?: return@screenModelScope
            updateState { current ->
                current.copy(
                    sets = (current.sets + source.copy(rowId = Uuid.random().toString())).toImmutableList()
                )
            }
            restTimer.start(source.exerciseId)
        }
    }

    private fun removeSet(rowId: String) {
        updateState { current ->
            current.copy(sets = current.sets.filterNot { it.rowId == rowId }.toImmutableList())
        }
    }

    private fun save() {
        screenModelScope { state ->
            if (!state.isSaveEnabled) return@screenModelScope
            val drafts = state.sets.map(::toDraft)
            val unparsed = state.sets.zip(drafts).firstOrNull { (row, draft) -> !isDraftValid(row, draft) }
            if (unparsed != null) {
                postSideEffect(
                    TrainingLogEditorSideEffect.ShowFailure(
                        RequestResult.Error(
                            kind = RequestFailure.Validation,
                            statusCode = null,
                            userMessage = getString(
                                Res.string.training_log_set_values_invalid,
                                unparsed.first.exerciseName,
                            ),
                            devMessage = "Не разобраны числовые поля подхода ${unparsed.first.rowId}",
                        )
                    )
                )
                return@screenModelScope
            }

            updateState { it.copy(isSaving = true) }
            val saved = trainingLogRepository.saveEntry(
                entryDate = entryDate,
                draft = TrainingLogDraft(
                    slotId = slotId,
                    notes = state.notes.trim().ifEmpty { null },
                    sets = drafts,
                ),
            )
            updateState { it.copy(isSaving = false) }
            when (saved) {
                is RequestResult.Error -> postSideEffect(TrainingLogEditorSideEffect.ShowFailure(saved))
                is RequestResult.Success -> when (val outcome = saved.data) {
                    is SaveOutcome.Sent -> {
                        showSaved(outcome.entry)
                        postSideEffect(TrainingLogEditorSideEffect.ShowSaved)
                    }
                    SaveOutcome.Queued -> postSideEffect(TrainingLogEditorSideEffect.ShowQueued)
                }
            }
        }
    }

    private suspend fun showSaved(entry: TrainingLogEntry) {
        val hintsByExercise = state.exercises.associate { it.exerciseId to it.lastResult }
        val savedRows = entry.sets.map { set -> toRow(set = set, hints = hintsByExercise[set.exerciseId]) }
        val savedNotes = entry.notes.orEmpty()
        syncedInput = TrainingDayInput(notes = savedNotes, rows = savedRows.map(::toInputRow))
        val volumeLabel = volumeFormat.toKilograms(entry.totalVolumeGrams)
        updateState { current ->
            current.copy(
                volumeLabel = volumeLabel,
                sets = savedRows.toImmutableList(),
                notes = savedNotes,
            )
        }
    }

    private fun toInput(state: TrainingLogEditorState): TrainingDayInput = TrainingDayInput(
        notes = state.notes,
        rows = state.sets.map(::toInputRow),
    )

    private fun toInputRow(row: SetRow): TrainingInputRow = TrainingInputRow(
        rowId = row.rowId,
        exerciseId = row.exerciseId,
        exerciseName = row.exerciseName,
        kind = row.kind,
        repetitionsText = row.repetitionsText,
        weightText = row.weightText,
        durationText = row.durationText,
        distanceText = row.distanceText,
    )

    private fun toRow(input: TrainingInputRow, hints: LastResultHints?): SetRow = SetRow(
        rowId = input.rowId,
        exerciseId = input.exerciseId,
        exerciseName = input.exerciseName,
        kind = input.kind,
        repetitionsText = input.repetitionsText,
        weightText = input.weightText,
        durationText = input.durationText,
        distanceText = input.distanceText,
        lastResult = hints ?: LastResultHints.Empty,
        isPersonalRecord = false,
    )

    private fun isDraftValid(row: SetRow, draft: TrainingSetDraft): Boolean = when (row.kind) {
        ExerciseKind.STRENGTH -> draft.repetitions != null && draft.weightGrams != null
        ExerciseKind.BODYWEIGHT -> draft.repetitions != null
        ExerciseKind.CARDIO -> draft.durationSeconds != null || draft.distanceMeters != null
    }

    private fun updateRow(rowId: String, change: (SetRow) -> SetRow) {
        updateState { current ->
            current.copy(
                sets = current.sets
                    .map { row -> if (row.rowId == rowId) change(row) else row }
                    .toImmutableList()
            )
        }
    }

    private fun emptyRowFor(exercise: ExerciseOption): SetRow = SetRow(
        rowId = Uuid.random().toString(),
        exerciseId = exercise.exerciseId,
        exerciseName = exercise.name,
        kind = exercise.kind,
        repetitionsText = "",
        weightText = "",
        durationText = "",
        distanceText = "",
        lastResult = exercise.lastResult,
        isPersonalRecord = false,
    )

    private fun toRow(set: TrainingSet, hints: LastResultHints?): SetRow = SetRow(
        rowId = set.id,
        exerciseId = set.exerciseId,
        exerciseName = set.exerciseName,
        kind = set.kind,
        repetitionsText = set.repetitions?.toString().orEmpty(),
        weightText = set.weightGrams?.let(weightInput::toKilogramsText).orEmpty(),
        durationText = set.durationSeconds?.let(durationInput::toMinutesText).orEmpty(),
        distanceText = set.distanceMeters?.toString().orEmpty(),
        lastResult = hints ?: LastResultHints.Empty,
        isPersonalRecord = set.isPersonalRecord,
    )

    private fun toHints(last: LastPerformed?): LastResultHints {
        if (last == null) return LastResultHints.Empty
        return LastResultHints(
            repetitions = last.repetitions?.toString().orEmpty(),
            weight = last.weightGrams?.let(weightInput::toKilogramsText).orEmpty(),
            duration = last.durationSeconds?.let(durationInput::toMinutesText).orEmpty(),
            distance = last.distanceMeters?.toString().orEmpty(),
        )
    }

    private suspend fun formatDate(date: LocalDate): String {
        val month = monthGenitiveOf(date)
        return "${date.day} $month"
    }

    private fun toDraft(row: SetRow): TrainingSetDraft = TrainingSetDraft(
        exerciseId = row.exerciseId,
        repetitions = row.repetitionsText.toIntOrNull(),
        weightGrams = weightInput.toGrams(row.weightText),
        durationSeconds = durationInput.toSeconds(row.durationText),
        distanceMeters = row.distanceText.toIntOrNull(),
    )
}
