package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder

private const val ADD_SET_LABEL = "+ Подход"
private const val PERSONAL_RECORD_LABEL = "рекорд"

enum class SetRowType { Strength, Bodyweight, Cardio }

data class SetRowValues(
    val repetitions: String,
    val weight: String,
    val duration: String,
    val distance: String,
)

data class SetRowHints(
    val repetitions: String,
    val weight: String,
    val duration: String,
    val distance: String,
) {

    companion object {

        val Empty = SetRowHints(repetitions = "", weight = "", duration = "", distance = "")
    }
}

data class SetRowCallbacks(
    val onRepetitionsChange: (String) -> Unit,
    val onWeightChange: (String) -> Unit,
    val onDurationChange: (String) -> Unit,
    val onDistanceChange: (String) -> Unit,
)

@Composable
fun AppSetRow(
    modifier: Modifier = Modifier,
    position: Int,
    type: SetRowType,
    values: SetRowValues,
    callbacks: SetRowCallbacks,
    hints: SetRowHints = SetRowHints.Empty,
    isPersonalRecord: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.sizing.setRowHeight),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.width(AppTheme.sizing.setNumberColumnWidth),
            text = position.toString(),
            style = AppTheme.typography.overline,
            color = AppTheme.colors.textMuted,
        )
        when (type) {
            SetRowType.Strength -> {
                AppNumericField(
                    modifier = Modifier.weight(1f),
                    value = values.repetitions,
                    onValueChange = callbacks.onRepetitionsChange,
                    unit = TextFieldUnit.Text("повт"),
                    placeholder = hints.repetitions,
                )
                AppNumericField(
                    modifier = Modifier.weight(1f),
                    value = values.weight,
                    onValueChange = callbacks.onWeightChange,
                    unit = TextFieldUnit.Text("кг"),
                    placeholder = hints.weight,
                )
            }
            SetRowType.Bodyweight -> AppNumericField(
                modifier = Modifier.weight(1f),
                value = values.repetitions,
                onValueChange = callbacks.onRepetitionsChange,
                unit = TextFieldUnit.Text("повт"),
                placeholder = hints.repetitions,
            )
            SetRowType.Cardio -> {
                AppNumericField(
                    modifier = Modifier.weight(1f),
                    value = values.duration,
                    onValueChange = callbacks.onDurationChange,
                    unit = TextFieldUnit.Text("мин"),
                    placeholder = hints.duration,
                )
                AppNumericField(
                    modifier = Modifier.weight(1f),
                    value = values.distance,
                    onValueChange = callbacks.onDistanceChange,
                    unit = TextFieldUnit.Text("м"),
                    placeholder = hints.distance,
                )
            }
        }
        if (isPersonalRecord) {
            PersonalRecordMark()
        }
    }
}

@Composable
private fun PersonalRecordMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(AppTheme.sizing.chipHeight)
            .background(
                color = AppTheme.colors.successSoft,
                shape = RoundedCornerShape(AppTheme.radius.pill),
            )
            .padding(horizontal = AppTheme.sizing.chipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = PERSONAL_RECORD_LABEL,
            style = AppTheme.typography.overline,
            color = AppTheme.colors.success,
        )
    }
}

@Composable
fun AppAddSetButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.sizing.setFieldHeight)
            .dashedBorder(
                color = AppTheme.colors.borderStrong,
                cornerRadius = AppTheme.radius.dp8,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ADD_SET_LABEL,
            style = AppTheme.typography.label,
            color = AppTheme.colors.accent,
        )
    }
}
