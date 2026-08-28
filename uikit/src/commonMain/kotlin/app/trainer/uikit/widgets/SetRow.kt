package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder
import app.trainer.uikit.resources.Res
import app.trainer.uikit.resources.set_add
import app.trainer.uikit.resources.set_personal_record
import app.trainer.uikit.resources.set_unit_distance
import app.trainer.uikit.resources.set_unit_duration
import app.trainer.uikit.resources.set_unit_repetitions
import app.trainer.uikit.resources.set_unit_weight
import org.jetbrains.compose.resources.stringResource

enum class SetRowType { Strength, Bodyweight, Cardio }

enum class SetRowState { Prefilled, Editing, Counted }

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
    state: SetRowState = SetRowState.Prefilled,
    onCount: (() -> Unit)? = null,
    countDescription: String = "",
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.radius.dp8)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.sizing.setRowHeight)
            .then(
                when (state) {
                    SetRowState.Prefilled ->
                        Modifier.border(AppTheme.borders.hairline, colors.border, shape)
                    SetRowState.Editing ->
                        Modifier
                            .background(color = colors.accentTint, shape = shape)
                            .border(AppTheme.borders.focus, colors.accent, shape)
                    SetRowState.Counted ->
                        Modifier.border(AppTheme.borders.hairline, colors.borderStrong, shape)
                }
            )
            .padding(horizontal = AppTheme.spacing.dp8),
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
                    unit = TextFieldUnit.Text(stringResource(Res.string.set_unit_repetitions)),
                    placeholder = hints.repetitions,
                )
                AppNumericField(
                    modifier = Modifier.weight(1f),
                    value = values.weight,
                    onValueChange = callbacks.onWeightChange,
                    unit = TextFieldUnit.Text(stringResource(Res.string.set_unit_weight)),
                    placeholder = hints.weight,
                )
            }
            SetRowType.Bodyweight -> AppNumericField(
                modifier = Modifier.weight(1f),
                value = values.repetitions,
                onValueChange = callbacks.onRepetitionsChange,
                unit = TextFieldUnit.Text(stringResource(Res.string.set_unit_repetitions)),
                placeholder = hints.repetitions,
            )
            SetRowType.Cardio -> {
                AppNumericField(
                    modifier = Modifier.weight(1f),
                    value = values.duration,
                    onValueChange = callbacks.onDurationChange,
                    unit = TextFieldUnit.Text(stringResource(Res.string.set_unit_duration)),
                    placeholder = hints.duration,
                )
                AppNumericField(
                    modifier = Modifier.weight(1f),
                    value = values.distance,
                    onValueChange = callbacks.onDistanceChange,
                    unit = TextFieldUnit.Text(stringResource(Res.string.set_unit_distance)),
                    placeholder = hints.distance,
                )
            }
        }
        if (isPersonalRecord) {
            PersonalRecordMark()
        }
        if (onCount != null) {
            CountMark(isCounted = state == SetRowState.Counted, description = countDescription, onClick = onCount)
        }
    }
}

@Composable
private fun CountMark(isCounted: Boolean, description: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(AppTheme.sizing.minTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(AppTheme.sizing.countMarkSize)
                .background(
                    color = if (isCounted) colors.successSoft else colors.bgSurface,
                    shape = RoundedCornerShape(AppTheme.radius.dp8),
                )
                .border(
                    width = AppTheme.borders.hairline,
                    color = if (isCounted) colors.success else colors.borderStrong,
                    shape = RoundedCornerShape(AppTheme.radius.dp8),
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                painter = AppIcons.sent,
                contentDescription = description,
                size = IconSize.Small,
                tint = if (isCounted) colors.success else colors.textMuted,
            )
        }
    }
}

@Composable
private fun PersonalRecordMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = AppTheme.sizing.chipHeight)
            .background(
                color = AppTheme.colors.successSoft,
                shape = RoundedCornerShape(AppTheme.radius.pill),
            )
            .padding(horizontal = AppTheme.sizing.chipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.set_personal_record),
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
            .heightIn(min = AppTheme.sizing.setFieldHeight)
            .dashedBorder(
                color = AppTheme.colors.borderStrong,
                cornerRadius = AppTheme.radius.dp8,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.set_add),
            style = AppTheme.typography.label,
            color = AppTheme.colors.accent,
        )
    }
}
