package app.trainer.feature.schedule.domain

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private const val DAYS_IN_WEEK = 7

class ScheduleWeeks {

    fun weekStartOf(date: LocalDate): LocalDate {
        return date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))
    }

    fun shiftWeeks(weekStart: LocalDate, weeks: Int): LocalDate {
        return weekStart.plus(DatePeriod(days = weeks * DAYS_IN_WEEK))
    }

    fun datesOf(weekStart: LocalDate): List<LocalDate> {
        return (0 until DAYS_IN_WEEK).map { offset -> weekStart.plus(DatePeriod(days = offset)) }
    }

    fun startInstant(weekStart: LocalDate, zone: TimeZone): Instant {
        return weekStart.atStartOfDayIn(zone)
    }

    fun endInstant(weekStart: LocalDate, zone: TimeZone): Instant {
        return weekStart.plus(DatePeriod(days = DAYS_IN_WEEK)).atStartOfDayIn(zone)
    }

    fun dateOf(instant: Instant, zone: TimeZone): LocalDate {
        return instant.toLocalDateTime(zone).date
    }

    fun parseZone(zoneId: String): TimeZone? {
        return runCatching { TimeZone.of(zoneId) }.getOrNull()
    }
}
