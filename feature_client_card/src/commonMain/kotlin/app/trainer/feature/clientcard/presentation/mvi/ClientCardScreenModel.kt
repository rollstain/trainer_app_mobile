package app.trainer.feature.clientcard.presentation.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.base.date.monthGenitiveOf
import app.trainer.base.input.WeightInput
import app.trainer.data.clients.ClientNote
import app.trainer.data.clients.ClientNoteDraft
import app.trainer.data.clients.ClientNotesRepository
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.program.AssignedProgram
import app.trainer.data.program.ProgramRepository
import app.trainer.data.program.ProgramSummary
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.Habit
import app.trainer.data.progress.HabitsRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.client_card_adherence
import app.trainer.strings.client_card_chest
import app.trainer.strings.client_card_done_count
import app.trainer.strings.client_card_hips
import app.trainer.strings.client_card_no_measurements
import app.trainer.strings.client_card_no_wellbeing
import app.trainer.strings.client_card_program_starts
import app.trainer.strings.client_card_sleep
import app.trainer.strings.client_card_waist
import app.trainer.strings.client_card_weight
import app.trainer.strings.client_card_wellbeing
import app.trainer.strings.programs_summary
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.getString

private const val CHECK_IN_HISTORY_DAYS = 90
private const val HABIT_HISTORY_DAYS = 6
private const val HABIT_WEEK_DAYS = 7
private const val MILLIMETERS_IN_CENTIMETER = 10
private const val SUMMARY_SEPARATOR = " · "

private const val DAYS_IN_WEEK = 7

class ClientCardScreenModel(
    private val clientUserId: String,
    private val notesRepository: ClientNotesRepository,
    private val checkInRepository: CheckInRepository,
    private val habitsRepository: HabitsRepository,
    private val participantsRepository: ParticipantsRepository,
    private val programRepository: ProgramRepository,
    private val weightInput: WeightInput,
) : BaseScreenModel<ClientCardState, ClientCardSideEffect, ClientCardEvent>(
    initialState = ClientCardState.initial(clientUserId = clientUserId),
) {

    init {
        onFetchData()
    }

    private val today: LocalDate get() = Clock.System.todayIn(TimeZone.currentSystemDefault())

    override fun onFetchData() {
        onFetchDataScope {
            loadNotes()
            loadProgress()
            loadProgram()
        }
    }

    override fun dispatch(event: ClientCardEvent) {
        when (event) {
            ClientCardEvent.OnRetryClicked -> onFetchData()
            ClientCardEvent.OnAddNoteClicked -> updateState { it.copy(editor = NoteEditor.forNewNote()) }
            ClientCardEvent.OnEditorDismissed -> updateState { it.copy(editor = null) }
            ClientCardEvent.OnEditorSaveClicked -> saveEditedNote()
            ClientCardEvent.OnEditorPinToggled -> updateEditor { it.copy(isPinned = !it.isPinned) }
            is ClientCardEvent.OnNoteClicked -> openEditor(event.noteId)
            is ClientCardEvent.OnNoteArchived -> archiveNote(event.noteId)
            is ClientCardEvent.OnEditorTitleChanged -> updateEditor { it.copy(title = event.title) }
            is ClientCardEvent.OnEditorDetailsChanged -> updateEditor { it.copy(details = event.details) }
            is ClientCardEvent.OnEditorKindChanged -> updateEditor { it.copy(kind = event.kind) }
            is ClientCardEvent.OnNewHabitTitleChanged -> updateState { it.copy(newHabitTitle = event.title) }
            ClientCardEvent.OnHabitAdded -> addHabit()
            is ClientCardEvent.OnReviewClicked -> openReview(event.checkInId)
            ClientCardEvent.OnReviewDismissed -> updateState { it.copy(reviewEditor = null) }
            ClientCardEvent.OnReviewSaveClicked -> saveReview()
            is ClientCardEvent.OnReviewCommentChanged -> updateState { current ->
                current.copy(reviewEditor = current.reviewEditor?.copy(comment = event.comment))
            }
            ClientCardEvent.OnAssignProgramClicked -> openProgramPicker()
            ClientCardEvent.OnProgramPickerDismissed -> updateState { it.copy(programPicker = null) }
            ClientCardEvent.OnProgramRemoved -> removeProgram()
            is ClientCardEvent.OnProgramStartSelected -> updateState { current ->
                current.copy(programPicker = current.programPicker?.copy(startsOn = event.start))
            }
            is ClientCardEvent.OnProgramPicked -> assignProgram(event.programId)
            ClientCardEvent.OnArchiveClientClicked -> updateState { it.copy(isArchiveDialogVisible = true) }
            ClientCardEvent.OnArchiveDismissed -> updateState { it.copy(isArchiveDialogVisible = false) }
            ClientCardEvent.OnArchiveConfirmed -> archiveClient()
        }
    }

    private suspend fun loadNotes() {
        updateState { it.copy(isLoading = true, failure = null) }
        when (val loaded = notesRepository.notesOf(clientUserId = clientUserId)) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = loaded) }
                postSideEffect(ClientCardSideEffect.ShowFailure(loaded))
            }
            is RequestResult.Success -> updateState { current ->
                current.copy(
                    notes = loaded.data.map(::toRow).toImmutableList(),
                    isLoading = false,
                    failure = null,
                )
            }
        }
    }

    private suspend fun loadProgress() {
        val checkIns = checkInRepository.clientCheckIns(
            clientUserId = clientUserId,
            from = today.minus(DatePeriod(days = CHECK_IN_HISTORY_DAYS)),
            to = today,
        )
        if (checkIns is RequestResult.Error) {
            postSideEffect(ClientCardSideEffect.ShowFailure(checkIns))
            return
        }
        val habits = habitsRepository.clientHabits(
            clientUserId = clientUserId,
            from = today.minus(DatePeriod(days = HABIT_HISTORY_DAYS)),
            to = today,
        )
        if (habits is RequestResult.Error) {
            postSideEffect(ClientCardSideEffect.ShowFailure(habits))
            return
        }
        val checkInRows = (checkIns as RequestResult.Success).data.map { toCheckInRow(it) }
        val habitRows = (habits as RequestResult.Success).data.map { toHabitRow(it) }
        updateState { current ->
            current.copy(
                checkIns = checkInRows.toImmutableList(),
                habits = habitRows.toImmutableList(),
            )
        }
    }

    private fun addHabit() {
        screenModelScope { state ->
            val title = state.newHabitTitle.trim()
            if (title.isEmpty()) return@screenModelScope
            val created = habitsRepository.createForClient(clientUserId = clientUserId, title = title)
            when (created) {
                is RequestResult.Error -> postSideEffect(ClientCardSideEffect.ShowFailure(created))
                is RequestResult.Success -> {
                    updateState { it.copy(newHabitTitle = "") }
                    loadProgress()
                }
            }
        }
    }

    private suspend fun toCheckInRow(checkIn: CheckIn): CheckInRow {
        val measurements = listOfNotNull(
            checkIn.weightGrams?.let { getString(Res.string.client_card_weight, weightInput.toKilogramsText(it)) },
            checkIn.waistMillimeters?.let { getString(Res.string.client_card_waist, it / MILLIMETERS_IN_CENTIMETER) },
            checkIn.chestMillimeters?.let { getString(Res.string.client_card_chest, it / MILLIMETERS_IN_CENTIMETER) },
            checkIn.hipsMillimeters?.let { getString(Res.string.client_card_hips, it / MILLIMETERS_IN_CENTIMETER) },
        )
        val wellbeing = listOfNotNull(
            checkIn.wellbeing?.let { getString(Res.string.client_card_wellbeing, it) },
            checkIn.sleepQuality?.let { getString(Res.string.client_card_sleep, it) },
            checkIn.adherence?.let { getString(Res.string.client_card_adherence, it) },
        )
        return CheckInRow(
            checkInId = checkIn.id,
            dateLabel = formatCardDate(checkIn.checkInDate),
            measurements = measurements.joinToString(separator = SUMMARY_SEPARATOR)
                .ifEmpty { getString(Res.string.client_card_no_measurements) },
            wellbeingLabel = wellbeing.joinToString(separator = SUMMARY_SEPARATOR)
                .ifEmpty { getString(Res.string.client_card_no_wellbeing) },
            notes = checkIn.notes,
            review = when {
                checkIn.isReviewed -> CheckInReview.Answered(comment = checkIn.coachComment)
                else -> CheckInReview.Awaiting
            },
            photos = checkIn.photos
                .map { photo -> CheckInPhotoRow(photoId = photo.id, url = photo.downloadUrl) }
                .toImmutableList(),
        )
    }

    private suspend fun toHabitRow(habit: Habit): ClientHabitRow = ClientHabitRow(
        habitId = habit.id,
        title = habit.title,
        doneCountLabel = getString(
            Res.string.client_card_done_count,
            habit.doneDates.size,
            HABIT_WEEK_DAYS,
        ),
        isSetByCoach = habit.isSetByCoach,
    )

    private suspend fun formatCardDate(date: LocalDate): String {
        val month = monthGenitiveOf(date).uppercase()
        return "${date.day} $month"
    }

    private fun openEditor(noteId: String) {
        screenModelScope { state ->
            val note = state.notes.firstOrNull { it.noteId == noteId } ?: return@screenModelScope
            updateState { current ->
                current.copy(
                    editor = NoteEditor(
                        editedNoteId = note.noteId,
                        kind = note.kind,
                        title = note.title,
                        details = note.details.orEmpty(),
                        isPinned = note.isPinned,
                        isSaving = false,
                    )
                )
            }
        }
    }

    private fun saveEditedNote() {
        screenModelScope { state ->
            val editor = state.editor ?: return@screenModelScope
            if (!editor.isSaveEnabled) return@screenModelScope

            updateEditor { it.copy(isSaving = true) }
            val draft = ClientNoteDraft(
                kind = editor.kind,
                title = editor.title.trim(),
                details = editor.details.trim().ifEmpty { null },
                isPinned = editor.isPinned,
            )
            val editedNoteId = editor.editedNoteId
            val saved = if (editedNoteId == null) {
                notesRepository.create(clientUserId = clientUserId, draft = draft)
            } else {
                notesRepository.update(noteId = editedNoteId, draft = draft)
            }
            when (saved) {
                is RequestResult.Error -> {
                    updateEditor { it.copy(isSaving = false) }
                    postSideEffect(ClientCardSideEffect.ShowFailure(saved))
                }
                is RequestResult.Success -> {
                    updateState { it.copy(editor = null) }
                    loadNotes()
                }
            }
        }
    }

    private fun archiveNote(noteId: String) {
        screenModelScope {
            when (val archived = notesRepository.archive(noteId = noteId)) {
                is RequestResult.Error -> postSideEffect(ClientCardSideEffect.ShowFailure(archived))
                is RequestResult.Success -> {
                    postSideEffect(ClientCardSideEffect.ShowNoteArchived)
                    loadNotes()
                }
            }
        }
    }

    private fun openReview(checkInId: String) {
        screenModelScope { state ->
            val row = state.checkIns.firstOrNull { it.checkInId == checkInId } ?: return@screenModelScope
            val comment = when (val review = row.review) {
                CheckInReview.Awaiting -> ""
                is CheckInReview.Answered -> review.comment.orEmpty()
            }
            updateState {
                it.copy(
                    reviewEditor = ReviewEditor(
                        checkInId = checkInId,
                        comment = comment,
                        isSaving = false,
                    )
                )
            }
        }
    }

    private fun saveReview() {
        screenModelScope { state ->
            val editor = state.reviewEditor ?: return@screenModelScope
            updateState { current -> current.copy(reviewEditor = current.reviewEditor?.copy(isSaving = true)) }
            val reviewed = checkInRepository.review(
                clientUserId = clientUserId,
                checkInId = editor.checkInId,
                comment = editor.comment.trim().ifEmpty { null },
            )
            when (reviewed) {
                is RequestResult.Error -> {
                    updateState { current ->
                        current.copy(reviewEditor = current.reviewEditor?.copy(isSaving = false))
                    }
                    postSideEffect(ClientCardSideEffect.ShowFailure(reviewed))
                }
                is RequestResult.Success -> {
                    updateState { it.copy(reviewEditor = null) }
                    loadProgress()
                }
            }
        }
    }

    private fun openProgramPicker() {
        screenModelScope {
            updateState {
                it.copy(
                    programPicker = ProgramPicker(
                        programs = persistentListOf(),
                        startsOn = ProgramStart.Today,
                        isLoading = true,
                        isSaving = false,
                    )
                )
            }
            when (val loaded = programRepository.programs()) {
                is RequestResult.Error -> {
                    updateState { it.copy(programPicker = null) }
                    postSideEffect(ClientCardSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> {
                    val rows = loaded.data.map { toPickRow(it) }
                    updateState { current ->
                        current.copy(
                            programPicker = current.programPicker?.copy(
                                programs = rows.toImmutableList(),
                                isLoading = false,
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun toPickRow(summary: ProgramSummary): ProgramPickRow = ProgramPickRow(
        programId = summary.id,
        title = summary.title,
        summary = getString(
            Res.string.programs_summary,
            summary.weeksCount,
            summary.filledDaysCount,
            summary.assignedClientsCount,
        ),
    )

    private fun assignProgram(programId: String) {
        screenModelScope { state ->
            val picker = state.programPicker ?: return@screenModelScope
            updateState { current ->
                current.copy(programPicker = current.programPicker?.copy(isSaving = true))
            }
            val assigned = programRepository.assign(
                programId = programId,
                clientUserId = clientUserId,
                startsOn = startDateOf(picker.startsOn),
            )
            when (assigned) {
                is RequestResult.Error -> {
                    updateState { current ->
                        current.copy(programPicker = current.programPicker?.copy(isSaving = false))
                    }
                    postSideEffect(ClientCardSideEffect.ShowFailure(assigned))
                }
                is RequestResult.Success -> {
                    updateState { it.copy(programPicker = null) }
                    loadProgram()
                }
            }
        }
    }

    private fun startDateOf(start: ProgramStart): LocalDate = when (start) {
        ProgramStart.Today -> today
        ProgramStart.NextMonday -> today.plus(
            DatePeriod(days = DAYS_IN_WEEK - today.dayOfWeek.isoDayNumber + 1)
        )
    }

    private fun removeProgram() {
        screenModelScope {
            when (val removed = programRepository.endAssignment(clientUserId = clientUserId)) {
                is RequestResult.Error -> postSideEffect(ClientCardSideEffect.ShowFailure(removed))
                is RequestResult.Success -> loadProgram()
            }
        }
    }

    private suspend fun loadProgram() {
        when (val loaded = programRepository.clientProgram(clientUserId = clientUserId)) {
            is RequestResult.Error -> updateState { it.copy(program = ClientProgramState.None) }
            is RequestResult.Success -> {
                val program = when (val assigned = loaded.data) {
                    AssignedProgram.None -> ClientProgramState.None
                    is AssignedProgram.Active -> ClientProgramState.Assigned(
                        title = assigned.program.programTitle,
                        startsLabel = getString(
                            Res.string.client_card_program_starts,
                            dayMonthOf(assigned.program.startsOn),
                        ),
                    )
                }
                updateState { it.copy(program = program) }
            }
        }
    }

    private fun archiveClient() {
        screenModelScope {
            updateState { it.copy(isArchiveDialogVisible = false, isArchiving = true) }
            val archived = participantsRepository.archiveClient(clientUserId = clientUserId)
            updateState { it.copy(isArchiving = false) }
            when (archived) {
                is RequestResult.Error -> postSideEffect(ClientCardSideEffect.ShowFailure(archived))
                is RequestResult.Success -> postSideEffect(ClientCardSideEffect.ClientArchived)
            }
        }
    }

    private fun updateEditor(change: (NoteEditor) -> NoteEditor) {
        updateState { current ->
            val editor = current.editor ?: return@updateState current
            current.copy(editor = change(editor))
        }
    }

    private fun toRow(note: ClientNote): NoteRow = NoteRow(
        noteId = note.id,
        kind = note.kind,
        title = note.title,
        details = note.details,
        isPinned = note.isPinned,
        updatedAtLabel = formatDate(note.updatedAt),
    )

    private fun formatDate(instant: Instant): String {
        val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val day = date.day.toString().padStart(length = 2, padChar = '0')
        val month = date.month.number.toString().padStart(length = 2, padChar = '0')
        return "$day.$month.${date.year}"
    }
}
