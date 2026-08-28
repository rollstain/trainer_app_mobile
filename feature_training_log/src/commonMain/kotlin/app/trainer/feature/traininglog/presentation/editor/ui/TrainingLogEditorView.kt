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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.data.traininglog.ExerciseKind
import app.trainer.feature.traininglog.presentation.editor.mvi.PlannedForDay
import app.trainer.feature.traininglog.presentation.editor.mvi.SetRow
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorEvent
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorState
import app.trainer.strings.Res
import app.trainer.strings.exercise_fields_bodyweight
import app.trainer.strings.exercise_fields_cardio
import app.trainer.strings.exercise_fields_strength
import app.trainer.strings.training_log_count_set
import app.trainer.strings.training_log_editor_done_action
import app.trainer.strings.training_log_editor_empty_description
import app.trainer.strings.training_log_editor_empty_title
import app.trainer.strings.training_log_editor_notes_label
import app.trainer.strings.training_log_editor_queued_banner
import app.trainer.strings.training_log_planned_apply
import app.trainer.strings.training_log_planned_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAddSetButton
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppOfflineBanner
import app.trainer.uikit.widgets.AppRestBar
import app.trainer.uikit.widgets.AppSetRow
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.SetRowCallbacks
import app.trainer.uikit.widgets.SetRowHints
import app.trainer.uikit.widgets.SetRowState
import app.trainer.uikit.widgets.SetRowType
import app.trainer.uikit.widgets.SetRowValues
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarSubtitle
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 3

@Composable
fun TrainingLogEditorView(
    modifier: Modifier = Modifier,
    state: TrainingLogEditorState,
    onEvent: (TrainingLogEditorEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = state.dateLabel,
            subtitle = TopBarSubtitle.Text(state.volumeLabel),
        )
        if (state.isQueued) {
            AppOfflineBanner(text = stringResource(Res.string.training_log_editor_queued_banner))
        }
        when (val planned = state.planned) {
            PlannedForDay.None -> Unit
            is PlannedForDay.Workout -> PlannedBanner(planned = planned, onEvent = onEvent)
        }
        ExercisePicker(state = state, onEvent = onEvent)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(TrainingLogEditorEvent.OnRetryClicked) },
                )
                state.isLoading -> AppCardShimmerList(
                    count = SHIMMER_CARDS,
                    lines = SHIMMER_CARD_LINES,
                )
                state.sets.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.training_log_editor_empty_title),
                    description = stringResource(Res.string.training_log_editor_empty_description),
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
            text = stringResource(Res.string.training_log_editor_done_action),
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
private fun PlannedBanner(planned: PlannedForDay.Workout, onEvent: (TrainingLogEditorEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.accentSoft)
            .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp8),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = stringResource(Res.string.training_log_planned_title, planned.dayTitle),
                style = AppTheme.typography.label,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = planned.summary,
                style = AppTheme.typography.numeric,
                color = AppTheme.colors.textSecondary,
            )
        }
        AppButton(
            text = stringResource(Res.string.training_log_planned_apply),
            onClick = { onEvent(TrainingLogEditorEvent.OnPlanApplied) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Small,
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
                label = TextFieldLabel.Text(stringResource(Res.string.training_log_editor_notes_label)),
            )
        }
    }
}

@Composable
private fun ExerciseCard(rows: List<SetRow>, onEvent: (TrainingLogEditorEvent) -> Unit) {
    val first = rows.first()
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
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
                    state = when {
                        row.isCounted -> SetRowState.Counted
                        row.isEmpty -> SetRowState.Prefilled
                        else -> SetRowState.Editing
                    },
                    onCount = { onEvent(TrainingLogEditorEvent.OnSetCounted(row.rowId)) },
                    countDescription = stringResource(Res.string.training_log_count_set),
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
}

@Composable
private fun kindLabel(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.STRENGTH -> stringResource(Res.string.exercise_fields_strength)
    ExerciseKind.BODYWEIGHT -> stringResource(Res.string.exercise_fields_bodyweight)
    ExerciseKind.CARDIO -> stringResource(Res.string.exercise_fields_cardio)
}.uppercase()

private fun toRowType(kind: ExerciseKind): SetRowType = when (kind) {
    ExerciseKind.STRENGTH -> SetRowType.Strength
    ExerciseKind.BODYWEIGHT -> SetRowType.Bodyweight
    ExerciseKind.CARDIO -> SetRowType.Cardio
}
