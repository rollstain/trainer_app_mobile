package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val SECTION_RULE_HEIGHT = 1.dp

sealed interface DaySectionSummary {

    data object None : DaySectionSummary

    data class Text(val value: String) : DaySectionSummary
}

@Composable
fun AppDaySectionHeader(
    modifier: Modifier = Modifier,
    dayLabel: String,
    isToday: Boolean,
    todayLabel: String,
    summary: DaySectionSummary = DaySectionSummary.None,
) {
    val labelColor = if (isToday) AppTheme.colors.accent else AppTheme.colors.textSecondary
    val ruleColor = if (isToday) AppTheme.colors.accent else AppTheme.colors.border
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.bgScreen)
            .heightIn(min = AppTheme.sizing.daySectionHeaderHeight)
            .padding(horizontal = AppTheme.spacing.dp16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = dayLabel,
            style = AppTheme.typography.numeric,
            color = labelColor,
        )
        if (isToday) {
            AppText(
                text = todayLabel,
                style = AppTheme.typography.overline,
                color = AppTheme.colors.accent,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(SECTION_RULE_HEIGHT)
                .background(ruleColor),
        )
        when (summary) {
            DaySectionSummary.None -> Unit
            is DaySectionSummary.Text -> AppText(
                text = summary.value,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
