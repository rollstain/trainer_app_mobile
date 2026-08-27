package app.trainer.feature.traininglog.presentation.programday.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.weekdayShortOf
import app.trainer.base.input.WeightInput
import app.trainer.data.program.ProgramDayDraft
import app.trainer.data.program.ProgramExerciseDraft
import app.trainer.data.program.ProgramExerciseLine
import app.trainer.data.program.ProgramRepository
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.toImmutableList

private const val DEFAULT_SETS = "3"

class ProgramDayScreenModel(
    private val programId: String,
    private val weekNumber: Int,
    private val dayOfWeek: Int,
    private val programRepository: ProgramRepository,
    private val trainingLogRepository: TrainingLogRepository,
    private val weightInput: WeightInput,
) : BaseScreenModel<ProgramDayState, ProgramDaySideEffect, ProgramDayEvent>(
    initialState = ProgramDayState.initial(
        programId = programId,
        weekNumber = weekNumber,
        dayOfWeek = dayOfWeek,
    ),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope { load() }
    }

    override fun dispatch(event: ProgramDayEvent) {
        when (event) {
            ProgramDayEvent.OnRetryClicked -> onFetchData()
            ProgramDayEvent.OnSaveClicked -> save()
            is ProgramDayEvent.OnTitleChanged -> updateState { it.copy(title = event.title) }
            is ProgramDayEvent.OnExerciseAdded -> addExercise(event.exerciseId)
            is ProgramDayEvent.OnLineRemoved -> updateLines { lines ->
                lines.filterIndexed { index, _ -> index != event.index }
            }
            is ProgramDayEvent.OnSetsChanged -> updateLine(event.index) { it.copy(setsText = event.text) }
            is ProgramDayEvent.OnRepetitionsChanged -> updateLine(event.index) {
                it.copy(repetitionsText = event.text)
            }
            is ProgramDayEvent.OnWeightChanged -> updateLine(event.index) { it.copy(weightText = event.text) }
        }
    }

    private fun updateLines(reduce: (List<ExerciseLineRow>) -> List<ExerciseLineRow>) {
        updateState { current -> current.copy(lines = reduce(current.lines).toImmutableList()) }
    }

    private fun updateLine(index: Int, reduce: (ExerciseLineRow) -> ExerciseLineRow) {
        updateLines { lines ->
            lines.mapIndexed { position, line -> if (position == index) reduce(line) else line }
        }
    }

    private fun addExercise(exerciseId: String) {
        screenModelScope { state ->
            val choice = state.choices.firstOrNull { it.exerciseId == exerciseId } ?: return@screenModelScope
            updateLines { lines ->
                lines + ExerciseLineRow(
                    exerciseId = choice.exerciseId,
                    exerciseName = choice.name,
                    setsText = DEFAULT_SETS,
                    repetitionsText = "",
                    weightText = "",
                )
            }
        }
    }

    private fun save() {
        screenModelScope { state ->
            if (!state.isSaveEnabled) return@screenModelScope
            updateState { it.copy(isSaving = true) }
            val saved = programRepository.saveDay(
                programId = programId,
                draft = ProgramDayDraft(
                    weekNumber = weekNumber,
                    dayOfWeek = dayOfWeek,
                    title = state.title.trim(),
                    exercises = state.lines.map(::toDraft),
                ),
            )
            updateState { it.copy(isSaving = false) }
            when (saved) {
                is RequestResult.Error -> postSideEffect(ProgramDaySideEffect.ShowFailure(saved))
                is RequestResult.Success -> postSideEffect(ProgramDaySideEffect.Saved)
            }
        }
    }

    private fun toDraft(line: ExerciseLineRow): ProgramExerciseDraft = ProgramExerciseDraft(
        exerciseId = line.exerciseId,
        setsCount = line.setsText.toInt(),
        repetitions = line.repetitionsText.toIntOrNull(),
        weightGrams = weightInput.toGrams(line.weightText),
        restSeconds = null,
        note = null,
    )

    private suspend fun load() {
        updateState { it.copy(isLoading = true, failure = null) }

        val exercises = trainingLogRepository.availableExercises()
        if (exercises is RequestResult.Error) return showFailure(exercises)

        val program = programRepository.program(programId = programId)
        if (program is RequestResult.Error) return showFailure(program)

        val day = (program as RequestResult.Success).data.days
            .firstOrNull { it.weekNumber == weekNumber && it.dayOfWeek == dayOfWeek }
        val dayLabel = weekdayShortOf(ordinal = dayOfWeek - 1)
        val choices = (exercises as RequestResult.Success).data
            .map { ExerciseChoice(exerciseId = it.id, name = it.name) }

        updateState { current ->
            current.copy(
                dayLabel = dayLabel,
                title = day?.title.orEmpty(),
                lines = day?.exercises.orEmpty().map(::toRow).toImmutableList(),
                choices = choices.toImmutableList(),
                isLoading = false,
                failure = null,
            )
        }
    }

    private fun toRow(line: ProgramExerciseLine): ExerciseLineRow = ExerciseLineRow(
        exerciseId = line.exerciseId,
        exerciseName = line.exerciseName,
        setsText = line.setsCount.toString(),
        repetitionsText = line.repetitions?.toString().orEmpty(),
        weightText = line.weightGrams?.let(weightInput::toKilogramsText).orEmpty(),
    )

    private suspend fun showFailure(failure: RequestResult.Error) {
        updateState { it.copy(isLoading = false, failure = failure) }
        postSideEffect(ProgramDaySideEffect.ShowFailure(failure))
    }
}
