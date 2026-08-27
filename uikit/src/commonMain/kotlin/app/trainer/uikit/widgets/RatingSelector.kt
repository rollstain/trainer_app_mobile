package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.uikit.AppTheme

private const val MIN_RATING = 1
private const val MAX_RATING = 5

@Composable
fun AppRatingSelector(
    modifier: Modifier = Modifier,
    selected: Int?,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        for (rating in MIN_RATING..MAX_RATING) {
            RatingCell(rating = rating, isSelected = rating == selected, onClick = { onSelect(rating) })
        }
    }
}

@Composable
private fun RatingCell(rating: Int, isSelected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.dp8)
    Box(
        modifier = Modifier
            .size(AppTheme.sizing.buttonLarge)
            .background(color = if (isSelected) colors.accent else colors.bgSurface, shape = shape)
            .border(
                width = AppTheme.borders.hairline,
                color = if (isSelected) colors.accent else colors.borderStrong,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rating.toString(),
            style = AppTheme.typography.numeric,
            color = if (isSelected) colors.accentOn else colors.textSecondary,
        )
    }
}
