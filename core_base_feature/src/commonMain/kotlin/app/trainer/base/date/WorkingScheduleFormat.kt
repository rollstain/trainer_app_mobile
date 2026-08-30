package app.trainer.base.date

import app.trainer.entities.WorkingDay
import app.trainer.strings.Res
import app.trainer.strings.working_hours_every_day
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.getString

private const val GROUP_SEPARATOR = " · "
private const val RUN_SEPARATOR = ", "
private const val RANGE_DASH = "—"
private const val DAYS_IN_WEEK = 7

suspend fun formatWorkingSchedule(workingHours: List<WorkingDay>): String {
    if (workingHours.isEmpty()) return ""
    val groups = hourGroupsOf(workingHours)
    val everyDay = groups.size == 1 && groups.single().days.size == DAYS_IN_WEEK
    if (everyDay) {
        return getString(Res.string.working_hours_every_day) + " " + groups.single().hoursLabel()
    }
    return groups.map { group -> group.daysLabel() + " " + group.hoursLabel() }.joinToString(GROUP_SEPARATOR)
}

private data class HourGroup(
    val days: List<DayOfWeek>,
    val opensAt: LocalTime,
    val closesAt: LocalTime,
)

private fun hourGroupsOf(workingHours: List<WorkingDay>): List<HourGroup> {
    val sorted = workingHours.sortedBy { it.dayOfWeek.ordinal }
    return sorted
        .groupBy { it.opensAt to it.closesAt }
        .map { (hours, days) ->
            HourGroup(
                days = days.map { it.dayOfWeek },
                opensAt = hours.first,
                closesAt = hours.second,
            )
        }
        .sortedBy { it.days.first().ordinal }
}

private fun HourGroup.hoursLabel(): String = "$opensAt$RANGE_DASH$closesAt"

private suspend fun HourGroup.daysLabel(): String {
    val runs = mutableListOf<MutableList<DayOfWeek>>()
    days.forEach { day ->
        val lastRun = runs.lastOrNull()
        if (lastRun != null && lastRun.last().ordinal == day.ordinal - 1) {
            lastRun += day
        } else {
            runs += mutableListOf(day)
        }
    }
    return runs.map { run -> run.label() }.joinToString(RUN_SEPARATOR)
}

private suspend fun List<DayOfWeek>.label(): String = when (size) {
    1 -> titleCasedDayOf(first())
    else -> titleCasedDayOf(first()) + RANGE_DASH + titleCasedDayOf(last())
}

private suspend fun titleCasedDayOf(dayOfWeek: DayOfWeek): String =
    weekdayShortOf(dayOfWeek.ordinal).lowercase().replaceFirstChar { it.titlecase() }
