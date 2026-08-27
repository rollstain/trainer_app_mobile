package app.trainer.feature.clientcard.presentation.diaries.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.dayMonthOf
import app.trainer.base.diary.DIARY_LAPSE_THRESHOLD_DAYS
import app.trainer.base.diary.DiaryLapse
import app.trainer.base.diary.diaryLapseOf
import app.trainer.base.format.VolumeFormat
import app.trainer.data.clients.CoachClient
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.traininglog.TrainingLogEntry
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.diaries_summary
import app.trainer.strings.diaries_window
import app.trainer.uikit.widgets.ComplianceCell
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private const val WINDOW_DAYS = 14
private const val HISTORY_DAYS = 90

class DiariesScreenModel(
    private val participantsRepository: ParticipantsRepository,
    private val trainingLogRepository: TrainingLogRepository,
    private val profileRepository: ProfileRepository,
    private val volumeFormat: VolumeFormat,
    private val weeks: ScheduleWeeks,
) : BaseScreenModel<DiariesState, DiariesSideEffect, DiariesEvent>(
    initialState = DiariesState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope { load() }
    }

    override fun dispatch(event: DiariesEvent) {
        when (event) {
            DiariesEvent.OnRetryClicked -> onFetchData()
            is DiariesEvent.OnPersonClicked -> openDiary(event.userId)
        }
    }

    private fun openDiary(userId: String) {
        screenModelScope { postSideEffect(DiariesSideEffect.OpenDiary(userId = userId)) }
    }

    private suspend fun load() {
        updateState { it.copy(isLoading = true, failure = null) }
        val profile = profileRepository.me()
        if (profile is RequestResult.Error) return showFailure(profile)
        val zone = (profile as RequestResult.Success).data.zoneId?.let(weeks::parseZone)
            ?: TimeZone.currentSystemDefault()
        val today = weeks.dateOf(Clock.System.now(), zone)

        val clients = participantsRepository.clientsOfCoach()
        if (clients is RequestResult.Error) return showFailure(clients)
        val windowStart = today.minus(DatePeriod(days = WINDOW_DAYS - 1))
        val historyStart = today.minus(DatePeriod(days = HISTORY_DAYS - 1))
        val entriesByClient = loadEntries(
            clients = (clients as RequestResult.Success).data,
            from = historyStart,
            to = today,
        )
        val rows = clients.data.map { client ->
            toRow(
                client = client,
                entries = entriesByClient[client.userId].orEmpty(),
                today = today,
                zone = zone,
            )
        }
        val windowLabel = getString(
            Res.string.diaries_window,
            WINDOW_DAYS,
            dayMonthOf(windowStart),
            dayMonthOf(today),
        )
        val byName = rows.sortedBy { it.displayName }
        updateState { current ->
            current.copy(
                lapsed = byName.filter { it.lapse.isLapsed() }.toImmutableList(),
                others = byName.filterNot { it.lapse.isLapsed() }.toImmutableList(),
                windowLabel = windowLabel,
                thresholdDays = DIARY_LAPSE_THRESHOLD_DAYS,
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun loadEntries(
        clients: List<CoachClient>,
        from: LocalDate,
        to: LocalDate,
    ): Map<String, List<TrainingLogEntry>> = coroutineScope {
        clients
            .map { client ->
                async {
                    val loaded = trainingLogRepository.clientEntries(
                        clientUserId = client.userId,
                        from = from,
                        to = to,
                    )
                    client.userId to when (loaded) {
                        is RequestResult.Success -> loaded.data
                        is RequestResult.Error -> emptyList()
                    }
                }
            }
            .awaitAll()
            .toMap()
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        updateState { it.copy(isLoading = false, failure = failure) }
        postSideEffect(DiariesSideEffect.ShowFailure(failure))
    }

    private suspend fun toRow(
        client: CoachClient,
        entries: List<TrainingLogEntry>,
        today: LocalDate,
        zone: TimeZone,
    ): DiaryRow {
        val loggedDates = entries.map { it.entryDate }.toSet()
        val windowDates = (0 until WINDOW_DAYS).map { offset ->
            today.minus(DatePeriod(days = WINDOW_DAYS - 1 - offset))
        }
        val windowEntries = entries.filter { it.entryDate in windowDates }
        val lapse = diaryLapseOf(
            today = today,
            lastEntryDate = loggedDates.maxOrNull(),
            linkedDate = client.linkedAt?.toLocalDate(zone),
        )
        return DiaryRow(
            userId = client.userId,
            displayName = client.displayName,
            lapse = lapse,
            cells = windowDates
                .map { date -> cellOf(date = date, loggedDates = loggedDates, lapse = lapse, today = today) }
                .toImmutableList(),
            summaryLabel = summaryOf(entries = windowEntries),
        )
    }

    private fun cellOf(
        date: LocalDate,
        loggedDates: Set<LocalDate>,
        lapse: DiaryLapse,
        today: LocalDate,
    ): ComplianceCell {
        if (lapse == DiaryLapse.NotStartedYet) return ComplianceCell.NotStarted
        val isLogged = date in loggedDates
        return when {
            date == today && isLogged -> ComplianceCell.TodayFilled
            date == today -> ComplianceCell.Today
            isLogged -> ComplianceCell.Filled
            else -> ComplianceCell.Empty
        }
    }

    private suspend fun summaryOf(entries: List<TrainingLogEntry>): String = getString(
        Res.string.diaries_summary,
        entries.size,
        volumeFormat.toTons(entries.sumOf { it.totalVolumeGrams }),
    )
}

private fun DiaryLapse.isLapsed(): Boolean = when (this) {
    is DiaryLapse.Lapsed, DiaryLapse.NeverLogged -> true
    DiaryLapse.Logging, DiaryLapse.NotStartedYet -> false
}

private fun Instant.toLocalDate(zone: TimeZone): LocalDate = toLocalDateTime(zone).date
