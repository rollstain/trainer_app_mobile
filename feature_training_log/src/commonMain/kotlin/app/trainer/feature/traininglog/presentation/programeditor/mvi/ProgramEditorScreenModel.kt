package app.trainer.feature.traininglog.presentation.programeditor.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.weekdayShortOf
import app.trainer.data.program.ProgramDay
import app.trainer.data.program.ProgramRepository
import app.trainer.data.program.TrainingProgram
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.program_day_summary
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.getString

private const val DAYS_IN_WEEK = 7

class ProgramEditorScreenModel(
    private val programId: String,
    private val programRepository: ProgramRepository,
) : BaseScreenModel<ProgramEditorState, ProgramEditorSideEffect, ProgramEditorEvent>(
    initialState = ProgramEditorState.initial(programId = programId),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope { load() }
    }

    override fun dispatch(event: ProgramEditorEvent) {
        when (event) {
            ProgramEditorEvent.OnRetryClicked -> onFetchData()
            is ProgramEditorEvent.OnWeekSelected -> selectWeek(event.weekNumber)
            is ProgramEditorEvent.OnDayClicked -> openDay(event.dayOfWeek)
        }
    }

    private fun selectWeek(weekNumber: Int) {
        screenModelScope {
            updateState { it.copy(selectedWeek = weekNumber) }
            load()
        }
    }

    private fun openDay(dayOfWeek: Int) {
        screenModelScope { state ->
            postSideEffect(
                ProgramEditorSideEffect.OpenDay(
                    programId = programId,
                    weekNumber = state.selectedWeek,
                    dayOfWeek = dayOfWeek,
                )
            )
        }
    }

    private suspend fun load() {
        updateState { it.copy(isLoading = true, failure = null) }
        when (val loaded = programRepository.program(programId = programId)) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = loaded) }
                postSideEffect(ProgramEditorSideEffect.ShowFailure(loaded))
            }
            is RequestResult.Success -> show(loaded.data)
        }
    }

    private suspend fun show(program: TrainingProgram) {
        val selectedWeek = state.selectedWeek.coerceIn(1, program.weeksCount)
        val daysOfWeek = program.days.filter { it.weekNumber == selectedWeek }.associateBy { it.dayOfWeek }
        val rows = (1..DAYS_IN_WEEK).map { dayOfWeek ->
            DayRow(
                dayOfWeek = dayOfWeek,
                label = weekdayShortOf(ordinal = dayOfWeek - 1),
                content = contentOf(daysOfWeek[dayOfWeek]),
            )
        }
        updateState { current ->
            current.copy(
                title = program.title,
                weeksCount = program.weeksCount,
                selectedWeek = selectedWeek,
                days = rows.toImmutableList(),
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun contentOf(day: ProgramDay?): DayContent {
        if (day == null || day.exercises.isEmpty()) return DayContent.Empty
        return DayContent.Filled(
            title = day.title,
            summary = getString(
                Res.string.program_day_summary,
                day.exercises.size,
                day.exercises.sumOf { it.setsCount },
            ),
        )
    }
}
