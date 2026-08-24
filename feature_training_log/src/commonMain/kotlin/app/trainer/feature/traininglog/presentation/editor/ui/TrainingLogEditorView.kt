package app.trainer.feature.traininglog.presentation.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.feature.traininglog.presentation.editor.mvi.SetRow
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorEvent
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorState
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAddSetButton
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppSetRow
import app.trainer.uikit.widgets.AppRestBar
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.SetRowCallbacks
import app.trainer.uikit.widgets.SetRowHints
import app.trainer.uikit.widgets.SetRowType
import app.trainer.uikit.widgets.SetRowValues
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.TopBarSubtitle

private const val NOTES_LABEL = "Заметка к дню"
private const val DONE_ACTION = "Готово"
private const val EMPTY_TITLE = "День пустой"
private const val EMPTY_DESCRIPTION =
    "Запишите первое упражнение — тренер увидит его сразу после сохранения."
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"

@Composable
fun TrainingLogEditorView(
    modifier: Modifier = Modifier,
    state: TrainingLogEditorState,
    onEvent: (TrainingLogEditorEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = state.dateLabel,
            leading = TopBarLeading.Back(onClick = onBackClick),
            subtitle = TopBarSubtitle.Text(state.volumeLabel),
        )
        ExercisePicker(state = state, onEvent = onEvent)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isFailed -> AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(TrainingLogEditorEvent.OnRetryClicked) },
                    ),
                )
                state.sets.isEmpty() && !state.isLoading -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EMPTY_TITLE,
                    description = EMPTY_DESCRIPTION,
                )
                else -> SetList(state = state, onEvent = onEvent)
            }
        }
        state.rest?.let { rest ->
            AppRestBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp8),
                label = rest.label,
                progress = rest.progress,
                onExtend = { onEvent(TrainingLogEditorEvent.OnRestExtended) },
                onSkip = { onEvent(TrainingLogEditorEvent.OnRestSkipped) },
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = DONE_ACTION,
            onClick = { onEvent(TrainingLogEditorEvent.OnSaveClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                state.isSaving -> ButtonState.Loading
                !state.isSaveEnabled -> ButtonState.Disabled
                else -> ButtonState.Idle
            },
        )
    }
}

@Composable
private fun ExercisePicker(
    state: TrainingLogEditorState,
    onEvent: (TrainingLogEditorEvent) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.dp16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        items(items = state.exercises, key = { it.exerciseId }) { exercise ->
            AppButton(
                text = exercise.name,
                onClick = { onEvent(TrainingLogEditorEvent.OnExerciseAdded(exercise.exerciseId)) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun SetList(state: TrainingLogEditorState, onEvent: (TrainingLogEditorEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        state.sets
            .groupBy { it.exerciseId }
            .forEach { (exerciseId, rows) ->
                item(key = "exercise-$exerciseId") {
                    ExerciseCard(rows = rows, onEvent = onEvent)
                }
            }
        item(key = "notes") {
            AppTextField(
                value = state.notes,
                onValueChange = { onEvent(TrainingLogEditorEvent.OnNotesChanged(it)) },
                kind = TextFieldKind.Multiline,
                label = TextFieldLabel.Text(NOTES_LABEL),
            )
        }
    }
}

@Composable
private fun ExerciseCard(rows: List<SetRow>, onEvent: (TrainingLogEditorEvent) -> Unit) {
    val first = rows.first()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppText(
                text = first.exerciseName,
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = kindLabel(first.kind),
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
            )
        }
        rows.forEachIndexed { index, row ->
            AppSetRow(
                position = index + 1,
                type = toRowType(row.kind),
                values = SetRowValues(
                    repetitions = row.repetitionsText,
                    weight = row.weightText,
                    duration = row.durationText,
                    distance = row.distanceText,
                ),
                hints = SetRowHints(
                    repetitions = row.lastResult.repetitions,
                    weight = row.lastResult.weight,
                    duration = row.lastResult.duration,
                    distance = row.lastResult.distance,
                ),
                isPersonalRecord = row.isPersonalRecord,
                callbacks = SetRowCallbacks(
                    onRepetitionsChange = {
                        onEvent(TrainingLogEditorEvent.OnRepetitionsChanged(row.rowId, it))
                    },
                    onWeightChange = {
                        onEvent(TrainingLogEditorEvent.OnWeightChanged(row.rowId, it))
                    },
                    onDurationChange = {
                        onEvent(TrainingLogEditorEvent.OnDurationChanged(row.rowId, it))
                    },
                    onDistanceChange = {
                        onEvent(TrainingLogEditorEvent.OnDistanceChanged(row.rowId, it))
                    },
                ),
            )
        }
        AppAddSetButton(
            onClick = { onEvent(TrainingLogEditorEvent.OnSetDuplicated(first.rowId)) },
        )
    }
}

private fun kindLabel(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.STRENGTH -> "ПОВТОРЫ И ВЕС"
    ExerciseKind.BODYWEIGHT -> "ТОЛЬКО ПОВТОРЫ"
    ExerciseKind.CARDIO -> "МИНУТЫ И МЕТРЫ"
}

private fun toRowType(kind: ExerciseKind): SetRowType = when (kind) {
    ExerciseKind.STRENGTH -> SetRowType.Strength
    ExerciseKind.BODYWEIGHT -> SetRowType.Bodyweight
    ExerciseKind.CARDIO -> SetRowType.Cardio
}
