package app.trainer.base.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.strings.Res
import app.trainer.strings.progress_dynamics_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppLineChart
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
fun MetricDynamicsCard(
    modifier: Modifier = Modifier,
    charts: ImmutableList<MetricChart>,
    chart: MetricChart,
    onMetricClick: (ProgressMetric) -> Unit,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = stringResource(Res.string.progress_dynamics_title),
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(
                        text = chart.latestLabel,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = chart.deltaLabel,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                items(items = charts, key = { it.metric.name }) { item ->
                    AppButton(
                        text = item.title,
                        onClick = { onMetricClick(item.metric) },
                        tone = if (item.metric == chart.metric) ButtonTone.Primary else ButtonTone.Secondary,
                        size = ButtonSize.Small,
                    )
                }
            }
            AppLineChart(
                values = chart.values,
                maxLabel = chart.maxLabel,
                minLabel = chart.minLabel,
                rangeLabel = chart.rangeLabel,
            )
        }
    }
}
