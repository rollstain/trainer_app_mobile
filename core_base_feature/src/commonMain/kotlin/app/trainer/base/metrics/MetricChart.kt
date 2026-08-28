package app.trainer.base.metrics

import app.trainer.base.date.monthGenitiveOf
import app.trainer.strings.Res
import app.trainer.strings.progress_no_change_label
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString

private const val CHART_MIN_POINTS = 2
private const val RANGE_SEPARATOR = " — "
private const val INCREASE_SIGN = "+"
private const val DECREASE_SIGN = "−"

enum class ProgressMetric { Weight, Waist, Chest, Hips, Wellbeing, Sleep }

data class MetricSample(val date: LocalDate, val value: Int)

data class MetricChart(
    val metric: ProgressMetric,
    val title: String,
    val values: ImmutableList<Float>,
    val maxLabel: String,
    val minLabel: String,
    val rangeLabel: String,
    val latestLabel: String,
    val deltaLabel: String,
)

suspend fun metricChartOf(
    metric: ProgressMetric,
    title: String,
    samples: List<MetricSample>,
    label: suspend (Int) -> String,
): MetricChart? {
    if (samples.size < CHART_MIN_POINTS) return null
    val ordered = samples.sortedBy { it.date }
    val values = ordered.map { it.value }
    val delta = values.last() - values.first()
    return MetricChart(
        metric = metric,
        title = title,
        values = values.map(Int::toFloat).toImmutableList(),
        maxLabel = label(values.max()),
        minLabel = label(values.min()),
        rangeLabel = dayAndMonthOf(ordered.first().date) +
            RANGE_SEPARATOR +
            dayAndMonthOf(ordered.last().date),
        latestLabel = label(values.last()),
        deltaLabel = when {
            delta > 0 -> INCREASE_SIGN + label(delta)
            delta < 0 -> DECREASE_SIGN + label(-delta)
            else -> getString(Res.string.progress_no_change_label)
        },
    )
}

private suspend fun dayAndMonthOf(date: LocalDate): String = "${date.day} ${monthGenitiveOf(date)}"
