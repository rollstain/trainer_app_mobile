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
private const val PAGE_SIZE = 30

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
                    booked = current.booked.withUnread(unreadByUserId).toImmutableList(),
                    others = current.others.withUnread(unreadByUserId).toImmutableList(),
                )
            }
        }
    }

    override fun dispatch(event: PeopleEvent) {
        when (event) {
            PeopleEvent.OnRetryClicked -> onFetchData()
            PeopleEvent.OnEndReached -> loadMore()
            PeopleEvent.OnCreateInviteClicked -> createInvite()
            is PeopleEvent.OnPersonClicked -> openPerson(event.userId)
        }
    }

    private suspend fun loadPeople() {
        updateState { it.copy(isLoading = true, failure = null) }
        val sessions = nextSessionsByClient()
        val booked = bookedRowsOf(sessions)
        if (booked == null) return

        when (val page = participantsRepository.clientsOfCoach(limit = PAGE_SIZE, after = null)) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = page) }
                postSideEffect(PeopleSideEffect.ShowFailure(page))
            }
            is RequestResult.Success -> {
                val bookedIds = booked.mapTo(mutableSetOf(), PersonRow::userId)
                val others = page.data.items
                    .filterNot { it.userId in bookedIds }
                    .map { client -> toRow(client = client, sessions = sessions) }
                updateState {
                    it.withFirstPage(booked = booked, others = others, nextCursor = page.data.nextCursor)
                }
            }
        }
    }

    private suspend fun bookedRowsOf(sessions: Map<String, NextSession>): List<PersonRow>? {
        if (sessions.isEmpty()) return emptyList()
        return when (val loaded = participantsRepository.clientsByIds(sessions.keys.toList())) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = loaded) }
                postSideEffect(PeopleSideEffect.ShowFailure(loaded))
                null
            }
            is RequestResult.Success ->
                loaded.data
                    .map { client -> toRow(client = client, sessions = sessions) }
                    .sortedBy { it.displayName }
        }
    }

    private fun loadMore() {
        screenModelScope { state ->
            val cursor = state.nextCursor ?: return@screenModelScope
            if (state.isLoadingMore) return@screenModelScope
            updateState { it.copy(isLoadingMore = true) }
            val sessions = nextSessionsByClient()
            when (val page = participantsRepository.clientsOfCoach(limit = PAGE_SIZE, after = cursor)) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingMore = false) }
                    postSideEffect(PeopleSideEffect.ShowFailure(page))
                }
                is RequestResult.Success -> {
                    val rows = page.data.items.map { client -> toRow(client = client, sessions = sessions) }
                    updateState { it.withNextPage(rows = rows, nextCursor = page.data.nextCursor) }
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

private fun List<PersonRow>.withUnread(unreadByUserId: Map<String, Long>): List<PersonRow> =
    map { row -> row.copy(unreadCount = unreadByUserId[row.userId] ?: 0) }
