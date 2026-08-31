package app.trainer.android

import app.trainer.base.date.filterTimeDigits
import app.trainer.base.date.formatTimeDigits
import app.trainer.base.date.parseTimeDigits
import app.trainer.base.date.timeDigitsOf
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalTime
import org.junit.Test

class TimeDigitsTest {

    @Test
    fun `a nine as the first digit becomes an oh-nine hour`() {
        assertEquals("09", filterTimeDigits("9"))
        assertEquals("0900", filterTimeDigits("9000"))
    }

    @Test
    fun `hours above twenty-three cannot be typed`() {
        assertEquals("2", filterTimeDigits("29"))
        assertEquals("2359", filterTimeDigits("2359"))
    }

    @Test
    fun `minute tens above five are dropped`() {
        assertEquals("09", filterTimeDigits("097"))
        assertEquals("0955", filterTimeDigits("0955"))
    }

    @Test
    fun `non-digits are ignored and length is capped`() {
        assertEquals("0930", filterTimeDigits("09:30:59"))
    }

    @Test
    fun `only four digits parse into a time`() {
        assertEquals(LocalTime(9, 30), parseTimeDigits("0930"))
        assertNull(parseTimeDigits("093"))
        assertNull(parseTimeDigits(""))
    }

    @Test
    fun `digits render with a colon and travel back from a time`() {
        assertEquals("09:30", formatTimeDigits("0930"))
        assertEquals("09", formatTimeDigits("09"))
        assertEquals("0905", timeDigitsOf(LocalTime(9, 5)))
    }
}
