package app.trainer.uikit.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.uikit.AppTheme

sealed interface SectionCount {

    data object None : SectionCount

    data class Value(val count: Int) : SectionCount
}

sealed interface SectionSummary {

    data object None : SectionSummary

    data class Text(val value: String) : SectionSummary
}

@Composable
fun AppSectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    count: SectionCount = SectionCount.None,
    summary: SectionSummary = SectionSummary.None,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppTheme.sizing.daySectionHeaderHeight)
            .padding(horizontal = AppTheme.spacing.dp16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = title,
            style = AppTheme.typography.label,
            color = AppTheme.colors.textPrimary,
        )
        AppText(
            modifier = Modifier.weight(1f),
            text = when (count) {
                SectionCount.None -> ""
                is SectionCount.Value -> count.count.toString()
            },
            style = AppTheme.typography.overline,
            color = AppTheme.colors.textMuted,
        )
        when (summary) {
            SectionSummary.None -> Unit
            is SectionSummary.Text -> AppText(
                text = summary.value,
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
            )
        }
    }
}
