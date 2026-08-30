package app.trainer.feature.account.workinghours.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.normalizeTimeText
import app.trainer.base.date.parseTimeText
import app.trainer.base.date.weekdayShortOf
import app.trainer.data.clients.CoachPolicy
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.entities.RequestResult
import app.trainer.entities.WorkingDay
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DayOfWeek

private const val DEFAULT_OPENS_TEXT = "09:00"
private const val DEFAULT_CLOSES_TEXT = "21:00"

class WorkingHoursScreenModel(
    private val participantsRepository: ParticipantsRepository,
) : BaseScreenModel<WorkingHoursState, WorkingHoursSideEffect, WorkingHoursEvent>(
    initialState = WorkingHoursState.initial(),
) {

    private var savedPolicy: CoachPolicy? = null
    private var savedRows: List<WorkingHourRow> = emptyList()

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val policy = participantsRepository.coachPolicy()) {
                is RequestResult.Error -> updateState { it.copy(isLoading = false, failure = policy) }
                is RequestResult.Success -> showSchedule(policy.data)
            }
        }
    }

    override fun dispatch(event: WorkingHoursEvent) {
        when (event) {
            WorkingHoursEvent.OnReloadRequested -> onFetchData()
            is WorkingHoursEvent.OnDayToggled -> changeRow(event.dayOfWeek) { toggled(it) }
            is WorkingHoursEvent.OnOpensChanged -> changeRow(event.dayOfWeek) {
                it.copy(opensText = normalizeTimeText(event.text), isPrefilled = false)
            }
            is WorkingHoursEvent.OnClosesChanged -> changeRow(event.dayOfWeek) {
                it.copy(closesText = normalizeTimeText(event.text), isPrefilled = false)
            }
            WorkingHoursEvent.OnApplyToAllClicked -> applyFirstHoursToAll()
            WorkingHoursEvent.OnSaveClicked -> save()
            WorkingHoursEvent.OnBackRequested -> requestLeave()
            WorkingHoursEvent.OnLeaveDialogDismissed -> updateState { it.copy(isLeaveDialogVisible = false) }
            WorkingHoursEvent.OnLeaveConfirmed -> screenModelScope {
                postSideEffect(WorkingHoursSideEffect.Close)
            }
        }
    }

    private suspend fun showSchedule(policy: CoachPolicy) {
        savedPolicy = policy
        val byDay = policy.workingHours.associateBy { it.dayOfWeek }
        val rows = DayOfWeek.entries.map { dayOfWeek ->
            val saved = byDay[dayOfWeek]
            WorkingHourRow(
                dayOfWeek = dayOfWeek,
                label = weekdayShortOf(dayOfWeek.ordinal),
                isWorking = saved != null,
                opensText = saved?.opensAt?.toString().orEmpty(),
                closesText = saved?.closesAt?.toString().orEmpty(),
                isPrefilled = false,
                isChanged = false,
                issue = null,
            )
        }
        savedRows = rows
        updateState {
            it.copy(
                rows = rows.toImmutableList(),
                changedDays = 0,
                isScheduleAbsent = policy.workingHours.isEmpty(),
                isLoading = false,
                failure = null,
            )
        }
    }

    private fun toggled(row: WorkingHourRow): WorkingHourRow {
        if (row.isWorking) return row.copy(isWorking = false)
        if (row.opensText.isNotEmpty() && row.closesText.isNotEmpty()) return row.copy(isWorking = true)
        val donor = hoursDonorFor(row.dayOfWeek)
        return row.copy(
            isWorking = true,
            opensText = donor?.opensText ?: DEFAULT_OPENS_TEXT,
            closesText = donor?.closesText ?: DEFAULT_CLOSES_TEXT,
            isPrefilled = true,
        )
    }

    private fun hoursDonorFor(dayOfWeek: DayOfWeek): WorkingHourRow? {
        val working = state.rows.filter { it.isWorking && it.opensText.isNotEmpty() }
        return working.lastOrNull { it.dayOfWeek.ordinal < dayOfWeek.ordinal }
            ?: working.firstOrNull { it.dayOfWeek.ordinal > dayOfWeek.ordinal }
    }

    private fun changeRow(dayOfWeek: DayOfWeek, change: (WorkingHourRow) -> WorkingHourRow) {
        updateState { current ->
            val rows = current.rows.map { row ->
                if (row.dayOfWeek == dayOfWeek) refreshed(change(row)) else row
            }
            current.copy(
                rows = rows.toImmutableList(),
                changedDays = rows.count { it.isChanged },
                isSaveFailed = false,
            )
        }
    }

    private fun refreshed(row: WorkingHourRow): WorkingHourRow =
        row.copy(isChanged = differsFromSaved(row), issue = issueOf(row))

    private fun differsFromSaved(row: WorkingHourRow): Boolean {
        val saved = savedRows.firstOrNull { it.dayOfWeek == row.dayOfWeek } ?: return true
        if (saved.isWorking != row.isWorking) return true
        if (!row.isWorking) return false
        return saved.opensText != row.opensText || saved.closesText != row.closesText
    }

    private fun issueOf(row: WorkingHourRow): WorkingHourIssue? {
        if (!row.isWorking) return null
        val opensAt = parseTimeText(row.opensText)
        val closesAt = parseTimeText(row.closesText)
        if (opensAt == null || closesAt == null) return WorkingHourIssue.Incomplete
        if (closesAt <= opensAt) return WorkingHourIssue.EndBeforeStart
        return null
    }

    private fun applyFirstHoursToAll() {
        updateState { current ->
            val donor = current.rows.firstOrNull { it.isWorking } ?: return@updateState current
            val rows = current.rows.map { row ->
                if (row.isWorking) {
                    refreshed(
                        row.copy(opensText = donor.opensText, closesText = donor.closesText, isPrefilled = false)
                    )
                } else {
                    row
                }
            }
            current.copy(rows = rows.toImmutableList(), changedDays = rows.count { it.isChanged })
        }
    }

    private fun requestLeave() {
        screenModelScope { current ->
            if (current.isDirty) {
                updateState { it.copy(isLeaveDialogVisible = true) }
            } else {
                postSideEffect(WorkingHoursSideEffect.Close)
            }
        }
    }

    private fun save() {
        screenModelScope { current ->
            val policy = savedPolicy ?: return@screenModelScope
            if (!current.isDirty || current.hasIssues || current.isSaving) return@screenModelScope
            val workingHours = current.rows.filter { it.isWorking }.mapNotNull { row ->
                val opensAt = parseTimeText(row.opensText) ?: return@mapNotNull null
                val closesAt = parseTimeText(row.closesText) ?: return@mapNotNull null
                WorkingDay(dayOfWeek = row.dayOfWeek, opensAt = opensAt, closesAt = closesAt)
            }
            updateState { it.copy(isSaving = true, isLeaveDialogVisible = false, isSaveFailed = false) }
            val saved = participantsRepository.updateCoachPolicy(
                policy = policy.copy(workingHours = workingHours)
            )
            updateState { it.copy(isSaving = false) }
            when (saved) {
                is RequestResult.Error -> {
                    updateState { it.copy(isSaveFailed = true) }
                    postSideEffect(WorkingHoursSideEffect.ShowFailure(saved))
                }
                is RequestResult.Success -> postSideEffect(WorkingHoursSideEffect.Saved)
            }
        }
    }
}
