package app.trainer.feature.schedule.presentation

import app.trainer.entities.WorkingDay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun isDayOff(date: LocalDate, workingDays: Set<DayOfWeek>): Boolean = if (workingDays.isEmpty()) {
    date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
} else {
    date.dayOfWeek !in workingDays
}

fun isOutsideWorkingHours(dayOfWeek: DayOfWeek, time: LocalTime, workingHours: List<WorkingDay>): Boolean {
    if (workingHours.isEmpty()) return false
    val day = workingHours.firstOrNull { it.dayOfWeek == dayOfWeek } ?: return true
    return time < day.opensAt || time >= day.closesAt
}
