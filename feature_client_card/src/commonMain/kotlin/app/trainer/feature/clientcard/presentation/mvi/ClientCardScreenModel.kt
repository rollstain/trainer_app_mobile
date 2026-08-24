package app.trainer.feature.clientcard.presentation.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.clients.ClientNote
import app.trainer.data.clients.ClientNoteDraft
import app.trainer.data.clients.ClientNotesRepository
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.Habit
import app.trainer.data.progress.HabitsRepository
import app.trainer.base.input.WeightInput
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

private const val CHECK_IN_HISTORY_DAYS = 90
private const val HABIT_HISTORY_DAYS = 6
private const val HABIT_WEEK_DAYS = 7
private const val MILLIMETERS_IN_CENTIMETER = 10
private const val SUMMARY_SEPARATOR = " · "
private const val NO_MEASUREMENTS = "без замеров"
private const val NO_WELLBEING = "самочувствие не отмечено"

private val CARD_MONTH_NAMES = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

class ClientCardScreenModel(
    private val clientUserId: String,
    private val notesRepository: ClientNotesRepository,
    private val checkInRepository: CheckInRepository,
    private val habitsRepository: HabitsRepository,
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
        }
    }

    private suspend fun loadNotes() {
        updateState { it.copy(isLoading = true, isFailed = false) }
        when (val loaded = notesRepository.notesOf(clientUserId = clientUserId)) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, isFailed = true) }
                postSideEffect(ClientCardSideEffect.ShowFailure(loaded))
            }
            is RequestResult.Success -> updateState { current ->
                current.copy(
                    notes = loaded.data.map(::toRow).toImmutableList(),
                    isLoading = false,
                    isFailed = false,
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
        updateState { current ->
            current.copy(
                checkIns = (checkIns as RequestResult.Success).data.map(::toCheckInRow).toImmutableList(),
                habits = (habits as RequestResult.Success).data.map(::toHabitRow).toImmutableList(),
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

    private fun toCheckInRow(checkIn: CheckIn): CheckInRow {
        val measurements = listOfNotNull(
            checkIn.weightGrams?.let { "${weightInput.toKilogramsText(it)} кг" },
            checkIn.waistMillimeters?.let { "талия ${it / MILLIMETERS_IN_CENTIMETER} см" },
            checkIn.chestMillimeters?.let { "грудь ${it / MILLIMETERS_IN_CENTIMETER} см" },
            checkIn.hipsMillimeters?.let { "бёдра ${it / MILLIMETERS_IN_CENTIMETER} см" },
        )
        val wellbeing = listOfNotNull(
            checkIn.wellbeing?.let { "самочувствие $it из 5" },
            checkIn.sleepQuality?.let { "сон $it из 5" },
        )
        return CheckInRow(
            checkInId = checkIn.id,
            dateLabel = formatCardDate(checkIn.checkInDate),
            measurements = measurements.joinToString(separator = SUMMARY_SEPARATOR).ifEmpty { NO_MEASUREMENTS },
            wellbeingLabel = wellbeing.joinToString(separator = SUMMARY_SEPARATOR).ifEmpty { NO_WELLBEING },
            notes = checkIn.notes,
            photos = checkIn.photos
                .map { photo -> CheckInPhotoRow(photoId = photo.id, url = photo.downloadUrl) }
                .toImmutableList(),
        )
    }

    private fun toHabitRow(habit: Habit): ClientHabitRow = ClientHabitRow(
        habitId = habit.id,
        title = habit.title,
        doneCountLabel = "${habit.doneDates.size} из $HABIT_WEEK_DAYS",
        isSetByCoach = habit.isSetByCoach,
    )

    private fun formatCardDate(date: LocalDate): String {
        val month = CARD_MONTH_NAMES[date.monthNumber - 1]
        return "${date.dayOfMonth} $month"
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
        val day = date.dayOfMonth.toString().padStart(length = 2, padChar = '0')
        val month = date.monthNumber.toString().padStart(length = 2, padChar = '0')
        return "$day.$month.${date.year}"
    }
}
