package app.trainer.feature.clientcard.presentation.people.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.base.date.weekdayShortOf
import app.trainer.base.diary.DiaryLapse
import app.trainer.base.diary.diaryLapseOf
import app.trainer.data.auth.AuthRepository
import app.trainer.data.chat.ChatRepository
import app.trainer.data.clients.CoachClient
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.schedule.CoachScheduleRepository
import app.trainer.data.schedule.SlotStatus
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.people_attention_diary
import app.trainer.strings.people_attention_missed
import app.trainer.strings.people_attention_never
import app.trainer.strings.people_next_session
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.getString

private const val SESSION_LOOKAHEAD_DAYS = 14
private const val DIARY_HISTORY_DAYS = 90
private const val MISSED_SESSIONS_THRESHOLD = 2
private const val PAGE_SIZE = 30
private const val SEARCH_DEBOUNCE_MILLIS = 300L

class PeopleScreenModel(
    private val participantsRepository: ParticipantsRepository,
    private val authRepository: AuthRepository,
    private val scheduleRepository: CoachScheduleRepository,
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val trainingLogRepository: TrainingLogRepository,
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
            is PeopleEvent.OnSearchChanged -> searchChanged(event.query)
            is PeopleEvent.OnPersonClicked -> openPerson(event.userId)
        }
    }

    private fun searchChanged(query: String) {
        searchJob?.cancel()
        updateState { it.copy(search = query) }
        searchJob = screenModelScope {
            delay(SEARCH_DEBOUNCE_MILLIS)
            loadPeople()
        }
    }

    private suspend fun loadPeople() {
        updateState { it.copy(isLoading = true, failure = null) }
        val query = state.search.trim().takeIf { it.isNotEmpty() }
        val sessions = nextSessionsByClient()
        val lapses = attentionByClient()
        val booked = if (query == null) bookedRowsOf(sessions = sessions, lapses = lapses) else emptyList()
        if (booked == null) return

        val loaded = participantsRepository.clientsOfCoach(limit = PAGE_SIZE, after = null, query = query)
        when (val page = loaded) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = page) }
                postSideEffect(PeopleSideEffect.ShowFailure(page))
            }
            is RequestResult.Success -> {
                val bookedIds = booked.mapTo(mutableSetOf(), PersonRow::userId)
                val others = page.data.items
                    .filterNot { it.userId in bookedIds }
                    .map { client -> toRow(client = client, sessions = sessions, lapses = lapses) }
                updateState {
                    it.withFirstPage(booked = booked, others = others, nextCursor = page.data.nextCursor)
                }
            }
        }
    }

    private suspend fun bookedRowsOf(
        sessions: Map<String, NextSession>,
        lapses: Map<String, String>,
    ): List<PersonRow>? {
        if (sessions.isEmpty()) return emptyList()
        return when (val loaded = participantsRepository.clientsByIds(sessions.keys.toList())) {
            is RequestResult.Error -> {
                updateState { it.copy(isLoading = false, failure = loaded) }
                postSideEffect(PeopleSideEffect.ShowFailure(loaded))
                null
            }
            is RequestResult.Success ->
                loaded.data
                    .map { client -> toRow(client = client, sessions = sessions, lapses = lapses) }
                    .sortedBy { it.displayName }
        }
    }

    private fun loadMore() {
        screenModelScope { state ->
            val cursor = state.nextCursor ?: return@screenModelScope
            if (state.isLoadingMore) return@screenModelScope
            updateState { it.copy(isLoadingMore = true) }
            val sessions = nextSessionsByClient()
            val lapses = attentionByClient()
            val loaded = participantsRepository.clientsOfCoach(
                limit = PAGE_SIZE,
                after = cursor,
                query = state.search.trim().takeIf { it.isNotEmpty() },
            )
            when (val page = loaded) {
                is RequestResult.Error -> {
                    updateState { it.copy(isLoadingMore = false) }
                    postSideEffect(PeopleSideEffect.ShowFailure(page))
                }
                is RequestResult.Success -> {
                    val rows = page.data.items.map { client ->
                        toRow(client = client, sessions = sessions, lapses = lapses)
                    }
                    updateState { it.withNextPage(rows = rows, nextCursor = page.data.nextCursor) }
                }
            }
        }
    }

    private var searchJob: Job? = null

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

    private fun toRow(
        client: CoachClient,
        sessions: Map<String, NextSession>,
        lapses: Map<String, String>,
    ): PersonRow {
        val session = sessions[client.userId]
        return PersonRow(
            userId = client.userId,
            displayName = client.displayName,
            hasMedicalNotes = client.hasMedicalNotes,
            nextSessionLabel = session?.label,
            hasPendingChangeRequest = session?.hasPendingChangeRequest == true,
            unreadCount = 0,
            attentionReason = lapses[client.userId],
        )
    }

    private suspend fun attentionByClient(): Map<String, String> {
        val missed = missedSessionsByClient()
        val lapsed = diaryLapsesByClient()
        return lapsed + missed
    }

    private suspend fun missedSessionsByClient(): Map<String, String> {
        val loaded = participantsRepository.missedSessions()
        if (loaded !is RequestResult.Success) return emptyMap()
        return loaded.data
            .filterValues { it >= MISSED_SESSIONS_THRESHOLD }
            .mapValues { (_, missed) -> getString(Res.string.people_attention_missed, missed) }
    }

    private suspend fun diaryLapsesByClient(): Map<String, String> {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        val summary = trainingLogRepository.coachDiarySummary(
            from = today.minus(DatePeriod(days = DIARY_HISTORY_DAYS)),
            to = today,
        )
        if (summary !is RequestResult.Success) return emptyMap()
        return summary.data.mapNotNull { client ->
            val lapse = diaryLapseOf(
                today = today,
                lastEntryDate = client.lastEntryDate,
                linkedDate = client.linkedAt?.toLocalDateTime(zone)?.date,
            )
            when (lapse) {
                is DiaryLapse.Lapsed -> client.clientUserId to getString(
                    Res.string.people_attention_diary,
                    lapse.days,
                )
                DiaryLapse.NeverLogged -> client.clientUserId to getString(Res.string.people_attention_never)
                DiaryLapse.Logging, DiaryLapse.NotStartedYet -> null
            }
        }.toMap()
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
            .filter { it.status != SlotStatus.CANCELLED && it.startsAt >= now }
            .flatMap { slot -> slot.participants.map { it.userId to slot } }
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
