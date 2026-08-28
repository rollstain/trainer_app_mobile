package app.trainer.feature.clientcard.presentation.people.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.data.auth.AuthRepository
import app.trainer.data.chat.ChatRepository
import app.trainer.data.clients.CoachClient
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.schedule.ScheduleRepository
import app.trainer.data.schedule.SlotStatus
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.people_next_session
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private const val SESSION_LOOKAHEAD_DAYS = 14

class PeopleScreenModel(
    private val participantsRepository: ParticipantsRepository,
    private val authRepository: AuthRepository,
    private val scheduleRepository: ScheduleRepository,
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<PeopleState, PeopleSideEffect, PeopleEvent>(
    initialState = PeopleState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            observeUnread()
        }
        screenModelScope {
            loadPeople()
        }
    }

    private suspend fun observeUnread() {
        chatRepository.observeDialogs().collectLatest { dialogs ->
            val unreadByUserId = dialogs.associate { it.peerUserId to it.unreadCount }
            updateState { current ->
                current.copy(
                    people = current.people
                        .map { row -> row.copy(unreadCount = unreadByUserId[row.userId] ?: 0) }
                        .toImmutableList(),
                )
            }
        }
    }

    override fun dispatch(event: PeopleEvent) {
        when (event) {
            PeopleEvent.OnRetryClicked -> onFetchData()
            PeopleEvent.OnCreateInviteClicked -> createInvite()
            is PeopleEvent.OnPersonClicked -> openPerson(event.userId)
        }
    }

    private suspend fun loadPeople() {
        updateState { it.copy(isLoading = true, failure = null) }
        when (val loaded = participantsRepository.clientsOfCoach()) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = loaded) }
                postSideEffect(PeopleSideEffect.ShowFailure(loaded))
            }
            is RequestResult.Success -> {
                val sessions = nextSessionsByClient()
                val rows = loaded.data.items.map { client -> toRow(client = client, sessions = sessions) }
                updateState { current ->
                    current.copy(
                        people = sortRows(rows).toImmutableList(),
                        isLoading = false,
                        failure = null,
                    )
                }
            }
        }
    }

    private fun createInvite() {
        screenModelScope {
            updateState { it.copy(isCreatingInvite = true) }
            val created = authRepository.createInvite()
            updateState { it.copy(isCreatingInvite = false) }
            when (created) {
                is RequestResult.Error -> postSideEffect(PeopleSideEffect.ShowFailure(created))
                is RequestResult.Success -> postSideEffect(
                    PeopleSideEffect.ShowInviteCode(code = created.data.code)
                )
            }
        }
    }

    private fun openPerson(userId: String) {
        screenModelScope {
            postSideEffect(PeopleSideEffect.OpenPerson(userId = userId))
        }
    }

    private fun toRow(client: CoachClient, sessions: Map<String, NextSession>): PersonRow {
        val session = sessions[client.userId]
        return PersonRow(
            userId = client.userId,
            displayName = client.displayName,
            hasMedicalNotes = client.hasMedicalNotes,
            nextSessionLabel = session?.label,
            hasPendingChangeRequest = session?.hasPendingChangeRequest == true,
            unreadCount = 0,
        )
    }

    private fun sortRows(rows: List<PersonRow>): List<PersonRow> = rows.sortedWith(
        compareBy<PersonRow> { it.nextSessionLabel == null }.thenBy { it.displayName },
    )

    private suspend fun nextSessionsByClient(): Map<String, NextSession> {
        val zone = coachZone() ?: return emptyMap()
        val now = Clock.System.now()
        val schedule = scheduleRepository.coachSchedule(
            from = now,
            to = now.plus(SESSION_LOOKAHEAD_DAYS, DateTimeUnit.DAY, zone),
        )
        if (schedule !is RequestResult.Success) return emptyMap()
        return schedule.data.slots
            .asSequence()
            .filter { it.status == SlotStatus.BOOKED && it.startsAt >= now }
            .mapNotNull { slot -> slot.clientUserId?.let { it to slot } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, slots) ->
                val earliest = slots.minBy { it.startsAt }
                NextSession(
                    label = formatSession(startsAt = earliest.startsAt, zone = zone),
                    hasPendingChangeRequest = earliest.pendingChangeRequestId != null,
                )
            }
    }

    private suspend fun coachZone(): TimeZone? {
        val profile = profileRepository.me()
        if (profile !is RequestResult.Success) return null
        val zoneId = profile.data.zoneId ?: return null
        return runCatching { TimeZone.of(zoneId) }.getOrNull()
    }

    private suspend fun formatSession(startsAt: Instant, zone: TimeZone): String {
        val dateTime = startsAt.toLocalDateTime(zone)
        val weekday = weekdayShortOf(dateTime.date).lowercase()
        return getString(
            Res.string.people_next_session,
            weekday,
            dayMonthOf(dateTime.date),
            timeOfDayOf(dateTime),
        )
    }

    private class NextSession(val label: String, val hasPendingChangeRequest: Boolean)
}
