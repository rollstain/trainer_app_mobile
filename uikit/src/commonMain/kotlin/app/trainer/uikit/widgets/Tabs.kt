package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val TABS_HEIGHT = 48.dp
private val ACTIVE_INDICATOR_HEIGHT = 2.dp

@Composable
fun <T> AppTabs(
    modifier: Modifier = Modifier,
    tabs: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = AppTheme.colors
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        tabs.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.defaultMinSize(minHeight = TABS_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    AppText(
                        text = labelOf(tab),
                        style = if (isSelected) AppTheme.typography.bodyStrong else AppTheme.typography.body,
                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSelected) ACTIVE_INDICATOR_HEIGHT else AppTheme.borders.hairline)
                        .background(if (isSelected) colors.accent else colors.border),
                )
            }
        }
    }
}
