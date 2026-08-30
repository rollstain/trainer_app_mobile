package app.trainer.feature.traininglog.presentation.programs.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.program.ProgramRepository
import app.trainer.data.program.ProgramSummary
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.programs_copy_title
import app.trainer.strings.programs_summary
import org.jetbrains.compose.resources.getString

private const val PAGE_SIZE = 20

class ProgramsScreenModel(
    private val programRepository: ProgramRepository,
) : BaseScreenModel<ProgramsState, ProgramsSideEffect, ProgramsEvent>(
    initialState = ProgramsState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope { load() }
    }

    override fun dispatch(event: ProgramsEvent) {
        when (event) {
            ProgramsEvent.OnRetryClicked -> onFetchData()
            ProgramsEvent.OnEndReached -> loadMore()
            ProgramsEvent.OnCreateClicked -> updateState { it.copy(draft = NewProgramDraft.empty()) }
            ProgramsEvent.OnDraftDismissed -> updateState { it.copy(draft = null) }
            ProgramsEvent.OnDraftSaveClicked -> createProgram()
            ProgramsEvent.OnWeekAdded -> updateDraft { it.copy(weeksCount = it.weeksCount + 1) }
            ProgramsEvent.OnWeekRemoved -> updateDraft { it.copy(weeksCount = it.weeksCount - 1) }
            is ProgramsEvent.OnDraftTitleChanged -> updateDraft { it.copy(title = event.title) }
            is ProgramsEvent.OnProgramClicked -> openProgram(event.programId)
            is ProgramsEvent.OnProgramArchived -> archiveProgram(event.programId)
            is ProgramsEvent.OnProgramDuplicated -> duplicateProgram(event.programId)
        }
    }

    private fun updateDraft(reduce: (NewProgramDraft) -> NewProgramDraft) {
        updateState { current -> current.copy(draft = current.draft?.let(reduce)) }
    }

    private fun openProgram(programId: String) {
        screenModelScope { postSideEffect(ProgramsSideEffect.OpenProgram(programId = programId)) }
    }

    private fun createProgram() {
        screenModelScope { state ->
            val draft = state.draft ?: return@screenModelScope
            if (!draft.isSaveEnabled) return@screenModelScope
            updateDraft { it.copy(isSaving = true) }
            val created = programRepository.create(
                title = draft.title.trim(),
                weeksCount = draft.weeksCount,
            )
            updateDraft { it.copy(isSaving = false) }
            when (created) {
                is RequestResult.Error -> postSideEffect(ProgramsSideEffect.ShowFailure(created))
                is RequestResult.Success -> {
                    updateState { it.copy(draft = null) }
                    postSideEffect(ProgramsSideEffect.OpenProgram(programId = created.data.id))
                }
            }
        }
    }

    private fun duplicateProgram(programId: String) {
        screenModelScope { state ->
            val source = state.programs.firstOrNull { it.programId == programId } ?: return@screenModelScope
            val duplicated = programRepository.duplicate(
                programId = programId,
                title = getString(Res.string.programs_copy_title, source.title),
            )
            when (duplicated) {
                is RequestResult.Error -> postSideEffect(ProgramsSideEffect.ShowFailure(duplicated))
                is RequestResult.Success -> postSideEffect(
                    ProgramsSideEffect.OpenProgram(programId = duplicated.data.id)
                )
            }
        }
    }

    private fun archiveProgram(programId: String) {
        screenModelScope {
            when (val archived = programRepository.archive(programId = programId)) {
                is RequestResult.Error -> postSideEffect(ProgramsSideEffect.ShowFailure(archived))
                is RequestResult.Success -> load()
            }
        }
    }

    private suspend fun load() {
        updateState { it.copy(isLoading = true, failure = null) }
        when (val loaded = programRepository.programs(limit = PAGE_SIZE, after = null)) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = loaded) }
                postSideEffect(ProgramsSideEffect.ShowFailure(loaded))
            }
            is RequestResult.Success -> {
                val rows = loaded.data.items.map { toRow(it) }
                updateState { it.withFirstPage(rows = rows, nextCursor = loaded.data.nextCursor) }
            }
        }
    }

    private fun loadMore() {
        screenModelScope { state ->
            val cursor = state.nextCursor ?: return@screenModelScope
            if (state.isLoadingMore) return@screenModelScope
            updateState { it.copy(isLoadingMore = true) }
            when (val page = programRepository.programs(limit = PAGE_SIZE, after = cursor)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingMore = false) }
                    postSideEffect(ProgramsSideEffect.ShowFailure(page))
                }
                is RequestResult.Success -> {
                    val rows = page.data.items.map { toRow(it) }
                    updateState { it.withNextPage(rows = rows, nextCursor = page.data.nextCursor) }
                }
            }
        }
    }

    private suspend fun toRow(summary: ProgramSummary): ProgramRow = ProgramRow(
        programId = summary.id,
        title = summary.title,
        summary = getString(
            Res.string.programs_summary,
            summary.weeksCount,
            summary.filledDaysCount,
            summary.assignedClientsCount,
        ),
    )
}
