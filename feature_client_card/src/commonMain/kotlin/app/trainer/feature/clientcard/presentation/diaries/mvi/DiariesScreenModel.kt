package app.trainer.feature.clientcard.presentation.diaries.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.ScheduleWeeks
import app.trainer.base.date.dayMonthOf
import app.trainer.base.diary.DIARY_LAPSE_THRESHOLD_DAYS
import app.trainer.base.diary.DiaryLapse
import app.trainer.base.diary.diaryLapseOf
import app.trainer.base.format.VolumeFormat
import app.trainer.data.profile.ProfileRepository
import app.trainer.data.traininglog.ClientDiarySummary
import app.trainer.data.traininglog.DiaryDay
import app.trainer.data.traininglog.TrainingLogRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.diaries_summary
import app.trainer.strings.diaries_window
import app.trainer.uikit.widgets.ComplianceCell
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

private const val WINDOW_DAYS = 14

class DiariesScreenModel(
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
            is DiariesEvent.OnSearchChanged -> updateState { it.copy(search = event.query) }
            is DiariesEvent.OnPersonClicked -> openDiary(event.userId)
        }
    }

    private fun openDiary(userId: String) {
        screenModelScope { postSideEffect(DiariesSideEffect.OpenDiary(userId = userId)) }
    }

    private suspend fun load() {
        updateState { it.copy(isLoading = it.isEmpty, failure = null) }
        val profile = profileRepository.me()
        if (profile is RequestResult.Error) return showFailure(profile)
        val zone = (profile as RequestResult.Success).data.zoneId?.let(weeks::parseZone)
            ?: TimeZone.currentSystemDefault()
        val today = weeks.dateOf(Clock.System.now(), zone)

        val windowStart = today.minus(DatePeriod(days = WINDOW_DAYS - 1))
        val summary = trainingLogRepository.coachDiarySummary(from = windowStart, to = today)
        if (summary is RequestResult.Error) return showFailure(summary)
        val rows = (summary as RequestResult.Success).data.map { client ->
            toRow(
                client = client,
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
        updateState { current ->
            current.copy(
                rows = rows.sortedBy { it.displayName }.toImmutableList(),
                windowLabel = windowLabel,
                thresholdDays = DIARY_LAPSE_THRESHOLD_DAYS,
                isLoading = false,
                failure = null,
            )
        }
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        updateState { it.copy(isLoading = false, failure = failure) }
        postSideEffect(DiariesSideEffect.ShowFailure(failure))
    }

    private suspend fun toRow(
        client: ClientDiarySummary,
        today: LocalDate,
        zone: TimeZone,
    ): DiaryRow {
        val loggedDates = client.days.map { it.entryDate }.toSet()
        val windowDates = (0 until WINDOW_DAYS).map { offset ->
            today.minus(DatePeriod(days = WINDOW_DAYS - 1 - offset))
        }
        val lapse = diaryLapseOf(
            today = today,
            lastEntryDate = client.lastEntryDate,
            linkedDate = client.linkedAt?.toLocalDate(zone),
        )
        return DiaryRow(
            userId = client.clientUserId,
            displayName = client.displayName,
            lapse = lapse,
            cells = windowDates
                .map { date -> cellOf(date = date, loggedDates = loggedDates, lapse = lapse, today = today) }
                .toImmutableList(),
            summaryLabel = summaryOf(days = client.days.filter { it.entryDate in windowDates }),
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

    private suspend fun summaryOf(days: List<DiaryDay>): String = getString(
        Res.string.diaries_summary,
        days.size,
        volumeFormat.toTons(days.sumOf { it.volumeGrams }),
    )
}

private fun Instant.toLocalDate(zone: TimeZone): LocalDate = toLocalDateTime(zone).date
