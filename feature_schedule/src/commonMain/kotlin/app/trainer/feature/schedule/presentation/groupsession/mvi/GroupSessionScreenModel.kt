package app.trainer.feature.schedule.presentation.groupsession.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.dayMonthOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.schedule.CoachScheduleRepository
import app.trainer.data.schedule.CoachSlot
import app.trainer.data.schedule.SlotStatus
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.group_session_booked_at
import app.trainer.strings.group_session_title
import app.trainer.strings.group_session_when
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

class GroupSessionScreenModel(
    private val slotId: String,
    private val scheduleRepository: CoachScheduleRepository,
    private val participantsRepository: ParticipantsRepository,
    private val profileRepository: ProfileRepository,
    private val weeks: ScheduleWeeks,
) : BaseScreenModel<GroupSessionState, GroupSessionSideEffect, GroupSessionEvent>(
    initialState = GroupSessionState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            updateState { it.copy(isLoading = true, failure = null) }
            when (val loaded = scheduleRepository.coachSlot(slotId)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoading = false, failure = loaded) }
                    postSideEffect(GroupSessionSideEffect.ShowFailure(loaded))
                }
                is RequestResult.Success -> show(loaded.data)
            }
        }
    }

    override fun dispatch(event: GroupSessionEvent) {
        when (event) {
            GroupSessionEvent.OnReloadRequested -> onFetchData()
            GroupSessionEvent.OnAddParticipantClicked -> openPicker()
            GroupSessionEvent.OnPickerDismissed -> updateState { it.copy(picker = null) }
            is GroupSessionEvent.OnClientPicked -> assign(event.clientUserId)
            is GroupSessionEvent.OnParticipantRemoved -> remove(event.clientUserId)
            is GroupSessionEvent.OnParticipantOpened -> screenModelScope {
                postSideEffect(GroupSessionSideEffect.OpenClientCard(event.clientUserId))
            }
            GroupSessionEvent.OnCompleteClicked -> complete()
            GroupSessionEvent.OnCancelClicked -> cancel()
        }
    }

    private suspend fun show(slot: CoachSlot) {
        val zone = coachZone()
        val startsAt = slot.startsAt.toLocalDateTime(zone)
        val endsAt = slot.startsAt.plus(slot.durationMinutes.minutes).toLocalDateTime(zone)
        val whenLabel = getString(
            Res.string.group_session_when,
            weekdayShortOf(startsAt.date),
            dayMonthOf(startsAt.date),
            timeOfDayOf(startsAt),
            timeOfDayOf(endsAt),
        )
        val participants = slot.participants.map { participant ->
            GroupParticipantRow(
                clientUserId = participant.userId,
                displayName = participant.displayName.orEmpty(),
                bookedAtLabel = getString(
                    Res.string.group_session_booked_at,
                    dayMonthOf(participant.bookedAt.toLocalDateTime(zone).date),
                ),
                hasMedicalNotes = participant.hasMedicalNotes,
            )
        }
        val waiting = slot.waitlist.map { entry ->
            GroupWaitingRow(
                clientUserId = entry.userId,
                displayName = entry.displayName.orEmpty(),
                joinedAtLabel = getString(
                    Res.string.group_session_booked_at,
                    dayMonthOf(entry.joinedAt.toLocalDateTime(zone).date),
                ),
            )
        }
        val title = getString(Res.string.group_session_title)
        updateState { current ->
            current.copy(
                title = title,
                whenLabel = whenLabel,
                takenSeats = slot.takenSeats,
                freeSeats = (slot.capacity - slot.takenSeats).coerceAtLeast(0),
                participants = participants.toImmutableList(),
                waiting = waiting.toImmutableList(),
                isCompleted = slot.status == SlotStatus.COMPLETED,
                isCancelled = slot.status == SlotStatus.CANCELLED,
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun coachZone(): TimeZone {
        val profile = profileRepository.me()
        if (profile !is RequestResult.Success) return TimeZone.currentSystemDefault()
        return profile.data.zoneId?.let(weeks::parseZone) ?: TimeZone.currentSystemDefault()
    }

    private fun openPicker() {
        screenModelScope {
            updateState { it.copy(isPickerLoading = true) }
            val loaded = participantsRepository.clientsOfCoach()
            updateState { it.copy(isPickerLoading = false) }
            when (loaded) {
                is RequestResult.Error -> postSideEffect(GroupSessionSideEffect.ShowFailure(loaded))
                is RequestResult.Success -> updateState { current ->
                    val booked = current.participants.map { it.clientUserId }.toSet()
                    current.copy(
                        picker = loaded.data.items
                            .filterNot { it.userId in booked }
                            .map { GroupPickRow(clientUserId = it.userId, displayName = it.displayName) }
                            .toImmutableList()
                    )
                }
            }
        }
    }

    private fun assign(clientUserId: String) {
        resolve { scheduleRepository.assignSlot(slotId = slotId, clientUserId = clientUserId) }
    }

    private fun remove(clientUserId: String) {
        resolve { scheduleRepository.removeParticipant(slotId = slotId, clientUserId = clientUserId) }
    }

    private fun complete() {
        resolve { scheduleRepository.completeSlot(slotId) }
    }

    private fun cancel() {
        resolve { scheduleRepository.cancelSlot(slotId) }
    }

    private fun resolve(call: suspend () -> RequestResult<CoachSlot>) {
        screenModelScope { current ->
            if (current.isResolving) return@screenModelScope
            updateState { it.copy(isResolving = true, picker = null) }
            val resolved = call()
            updateState { it.copy(isResolving = false) }
            when (resolved) {
                is RequestResult.Error -> postSideEffect(GroupSessionSideEffect.ShowFailure(resolved))
                is RequestResult.Success -> {
                    show(resolved.data)
                    postSideEffect(GroupSessionSideEffect.SessionChanged)
                }
            }
        }
    }
}
