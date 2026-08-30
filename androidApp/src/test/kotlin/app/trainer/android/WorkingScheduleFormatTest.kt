package app.trainer.android

import app.trainer.base.date.formatWorkingSchedule
import app.trainer.entities.WorkingDay
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "ru-rRU")
class WorkingScheduleFormatTest {

    @Test
    fun `consecutive days with equal hours collapse into a range`() = runTest {
        val label = formatWorkingSchedule(
            listOf(
                day(DayOfWeek.MONDAY, "09:00", "21:00"),
                day(DayOfWeek.TUESDAY, "09:00", "21:00"),
                day(DayOfWeek.WEDNESDAY, "09:00", "21:00"),
                day(DayOfWeek.THURSDAY, "09:00", "21:00"),
                day(DayOfWeek.FRIDAY, "09:00", "19:00"),
                day(DayOfWeek.SATURDAY, "10:30", "15:00"),
            )
        )

        assertEquals("Пн—Чт 09:00—21:00 · Пт 09:00—19:00 · Сб 10:30—15:00", label)
    }

    @Test
    fun `seven equal days read as every day`() = runTest {
        val label = formatWorkingSchedule(
            DayOfWeek.entries.map { day(it, "08:00", "22:00") }
        )

        assertEquals("Каждый день 08:00—22:00", label)
    }

    @Test
    fun `non-adjacent days with equal hours are listed with commas`() = runTest {
        val label = formatWorkingSchedule(
            listOf(
                day(DayOfWeek.MONDAY, "07:00", "12:00"),
                day(DayOfWeek.WEDNESDAY, "07:00", "12:00"),
                day(DayOfWeek.FRIDAY, "07:00", "12:00"),
                day(DayOfWeek.SATURDAY, "10:00", "14:00"),
            )
        )

        assertEquals("Пн, Ср, Пт 07:00—12:00 · Сб 10:00—14:00", label)
    }

    @Test
    fun `an empty schedule formats to nothing`() = runTest {
        assertEquals("", formatWorkingSchedule(emptyList()))
    }

    private fun day(dayOfWeek: DayOfWeek, opensAt: String, closesAt: String) = WorkingDay(
        dayOfWeek = dayOfWeek,
        opensAt = LocalTime.parse(opensAt),
        closesAt = LocalTime.parse(closesAt),
    )
}
