package app.trainer.android

import app.trainer.entities.WorkingDay
import app.trainer.feature.schedule.presentation.isDayOff
import app.trainer.feature.schedule.presentation.isOutsideWorkingHours
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test

private val SATURDAY = LocalDate(2026, 9, 5)
private val SUNDAY = LocalDate(2026, 9, 6)
private val OPENS_AT = LocalTime(8, 0)
private val CLOSES_AT = LocalTime(20, 0)

class WorkingScheduleTest {

    @Test
    fun `without a schedule saturday and sunday stay days off`() {
        assertTrue(isDayOff(date = SATURDAY, workingDays = emptySet()))
        assertTrue(isDayOff(date = SUNDAY, workingDays = emptySet()))
        assertFalse(isDayOff(date = SATURDAY.minusDays(1), workingDays = emptySet()))
    }

    @Test
    fun `with a schedule only the missing days are off`() {
        val workingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)

        assertFalse(isDayOff(date = SATURDAY, workingDays = workingDays))
        assertTrue(isDayOff(date = SUNDAY, workingDays = workingDays))
    }

    @Test
    fun `the opening hour is inside and the closing hour is already outside`() {
        val workingHours = listOf(
            WorkingDay(dayOfWeek = DayOfWeek.MONDAY, opensAt = OPENS_AT, closesAt = CLOSES_AT),
        )

        assertFalse(isOutsideWorkingHours(DayOfWeek.MONDAY, OPENS_AT, workingHours))
        assertTrue(isOutsideWorkingHours(DayOfWeek.MONDAY, CLOSES_AT, workingHours))
        assertTrue(isOutsideWorkingHours(DayOfWeek.MONDAY, LocalTime(7, 59), workingHours))
        assertTrue(isOutsideWorkingHours(DayOfWeek.TUESDAY, OPENS_AT, workingHours))
    }

    @Test
    fun `an empty schedule never warns`() {
        assertFalse(isOutsideWorkingHours(DayOfWeek.SUNDAY, LocalTime(3, 0), emptyList()))
    }
}

private fun LocalDate.minusDays(days: Int): LocalDate = LocalDate.fromEpochDays(toEpochDays() - days)
