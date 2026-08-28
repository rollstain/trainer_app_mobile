package app.trainer.android

import app.trainer.base.metrics.MetricSample
import app.trainer.base.metrics.ProgressMetric
import app.trainer.base.metrics.metricChartOf
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val WEIGHT_TITLE = "Weight"
private const val NO_CHANGE_LABEL = "no change"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MetricChartTest {

    @Test
    fun `a single measurement is not a chart yet`() = runTest {
        val chart = weightChartOf(august(day = 1) to 84_000)

        assertNull(chart, "по одной точке динамику не построить")
    }

    @Test
    fun `losing weight reads as a decrease`() = runTest {
        val chart = weightChartOf(
            august(day = 1) to 84_000,
            august(day = 10) to 83_000,
            august(day = 24) to 82_000,
        )

        assertEquals("−2000", chart?.deltaLabel)
        assertEquals("82000", chart?.latestLabel)
    }

    @Test
    fun `gaining weight reads as an increase`() = runTest {
        val chart = weightChartOf(august(day = 1) to 82_000, august(day = 24) to 84_000)

        assertEquals("+2000", chart?.deltaLabel)
    }

    @Test
    fun `the same weight twice reads as no change`() = runTest {
        val chart = weightChartOf(august(day = 1) to 82_000, august(day = 24) to 82_000)

        assertEquals(NO_CHANGE_LABEL, chart?.deltaLabel)
    }

    @Test
    fun `the freshest measurement is the latest one whatever order it arrives in`() = runTest {
        val newestFirst = weightChartOf(august(day = 24) to 82_000, august(day = 1) to 84_000)

        assertEquals("82000", newestFirst?.latestLabel, "свежая точка — последняя на графике")
        assertEquals("−2000", newestFirst?.deltaLabel)
        assertEquals("1 August — 24 August", newestFirst?.rangeLabel)
    }

    @Test
    fun `the extremes come from the whole series, not from its ends`() = runTest {
        val chart = weightChartOf(
            august(day = 1) to 83_000,
            august(day = 10) to 86_000,
            august(day = 24) to 82_000,
        )

        assertEquals("86000", chart?.maxLabel)
        assertEquals("82000", chart?.minLabel)
    }

    private suspend fun weightChartOf(vararg points: Pair<LocalDate, Int>) = metricChartOf(
        metric = ProgressMetric.Weight,
        title = WEIGHT_TITLE,
        samples = points.map { MetricSample(date = it.first, value = it.second) },
        label = Int::toString,
    )

    private fun august(day: Int): LocalDate = LocalDate(year = 2026, month = 8, day = day)
}
