package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.trainer.uikit.AppTheme
import kotlinx.collections.immutable.ImmutableList

enum class HabitWeekDay { Done, Missed, Future }

@Composable
fun AppHabitWeekBar(
    modifier: Modifier = Modifier,
    days: ImmutableList<HabitWeekDay>,
    weekdayLabels: ImmutableList<String>,
) {
    val shape = RoundedCornerShape(AppTheme.radius.dp4)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
        days.forEachIndexed { index, day ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AppTheme.sizing.habitWeekCell)
                    .background(color = fillOf(day), shape = shape)
                    .then(outlineModifier(day = day, shape = shape)),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = weekdayLabels.getOrElse(index) { "" },
                    style = AppTheme.typography.overline,
                    color = contentOf(day),
                )
            }
        }
    }
}

@Composable
private fun fillOf(day: HabitWeekDay): Color = when (day) {
    HabitWeekDay.Done -> AppTheme.colors.success
    HabitWeekDay.Missed -> AppTheme.colors.bgSurfaceSunken
    HabitWeekDay.Future -> Color.Transparent
}

@Composable
private fun contentOf(day: HabitWeekDay): Color = when (day) {
    HabitWeekDay.Done -> AppTheme.colors.accentOn
    HabitWeekDay.Missed -> AppTheme.colors.textSecondary
    HabitWeekDay.Future -> AppTheme.colors.textMuted
}

@Composable
private fun outlineModifier(day: HabitWeekDay, shape: RoundedCornerShape): Modifier = when (day) {
    HabitWeekDay.Future -> Modifier.border(
        width = AppTheme.borders.hairline,
        color = AppTheme.colors.border,
        shape = shape,
    )
    HabitWeekDay.Done, HabitWeekDay.Missed -> Modifier
}
