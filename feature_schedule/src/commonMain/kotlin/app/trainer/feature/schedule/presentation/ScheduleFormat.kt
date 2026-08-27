package app.trainer.feature.schedule.presentation

import app.trainer.base.date.monthGenitiveOf
import kotlinx.datetime.LocalDate

internal suspend fun formatScheduleDate(date: LocalDate): String =
    "${date.day} ${monthGenitiveOf(date)}"

internal suspend fun formatScheduleWeekTitle(weekStart: LocalDate, weekEnd: LocalDate): String {
    val endMonth = monthGenitiveOf(weekEnd)
    if (weekStart.month == weekEnd.month) {
        return "${weekStart.day}—${weekEnd.day} $endMonth"
    }
    val startMonth = monthGenitiveOf(weekStart)
    return "${weekStart.day} $startMonth — ${weekEnd.day} $endMonth"
}
