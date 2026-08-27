package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trainer.uikit.AppTheme
import kotlinx.collections.immutable.ImmutableList

enum class ComplianceCell { Filled, Empty, Today, TodayFilled, NotStarted }

@Composable
fun AppComplianceStrip(
    modifier: Modifier = Modifier,
    cells: ImmutableList<ComplianceCell>,
) {
    val shape = RoundedCornerShape(AppTheme.radius.dp4)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
        cells.forEach { cell ->
            Box(
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .widthIn(max = AppTheme.sizing.complianceCell)
                    .fillMaxWidth()
                    .height(AppTheme.sizing.complianceCell)
                    .background(color = fillOf(cell), shape = shape)
                    .then(outlineModifier(cell = cell, shape = shape)),
            )
        }
    }
}

@Composable
private fun fillOf(cell: ComplianceCell): androidx.compose.ui.graphics.Color = when (cell) {
    ComplianceCell.Filled, ComplianceCell.TodayFilled -> AppTheme.colors.accent
    ComplianceCell.Empty, ComplianceCell.Today -> AppTheme.colors.bgSurfaceSunken
    ComplianceCell.NotStarted -> AppTheme.colors.bgScreen
}

@Composable
private fun outlineModifier(cell: ComplianceCell, shape: RoundedCornerShape): Modifier = when (cell) {
    ComplianceCell.Today -> Modifier.border(
        width = AppTheme.borders.field,
        color = AppTheme.colors.borderStrong,
        shape = shape,
    )
    ComplianceCell.TodayFilled -> Modifier.border(
        width = AppTheme.borders.field,
        color = AppTheme.colors.accentSoft,
        shape = shape,
    )
    ComplianceCell.NotStarted -> Modifier.border(
        width = AppTheme.borders.hairline,
        color = AppTheme.colors.border,
        shape = shape,
    )
    ComplianceCell.Filled, ComplianceCell.Empty -> Modifier
}
