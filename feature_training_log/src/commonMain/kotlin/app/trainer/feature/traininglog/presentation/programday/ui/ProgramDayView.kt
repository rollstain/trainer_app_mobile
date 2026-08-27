package app.trainer.feature.traininglog.presentation.programday.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.traininglog.presentation.programday.mvi.ExerciseLineRow
import app.trainer.feature.traininglog.presentation.programday.mvi.ProgramDayEvent
import app.trainer.feature.traininglog.presentation.programday.mvi.ProgramDayState
import app.trainer.strings.Res
import app.trainer.strings.program_day_add_hint
import app.trainer.strings.program_day_remove_action
import app.trainer.strings.program_day_reps_label
import app.trainer.strings.program_day_save_action
import app.trainer.strings.program_day_sets_label
import app.trainer.strings.program_day_title_label
import app.trainer.strings.program_day_weight_label
import app.trainer.strings.program_week_chip
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.TopBarSubtitle
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 2

@Composable
fun ProgramDayView(
    state: ProgramDayState,
    onEvent: (ProgramDayEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = state.dayLabel,
            subtitle = TopBarSubtitle.Text(stringResource(Res.string.program_week_chip, state.weekNumber)),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ProgramDayEvent.OnRetryClicked) },
                )
                else -> DayContent(state = state, onEvent = onEvent)
            }
        }
        if (!state.isLoading && state.failure == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(AppTheme.spacing.dp16),
            ) {
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.program_day_save_action),
                    onClick = { onEvent(ProgramDayEvent.OnSaveClicked) },
                    tone = ButtonTone.Primary,
                    size = ButtonSize.Large,
                    state = when {
                        state.isSaving -> ButtonState.Loading
                        state.isSaveEnabled -> ButtonState.Idle
                        else -> ButtonState.Disabled
                    },
                )
            }
        }
    }
}

@Composable
private fun DayContent(state: ProgramDayState, onEvent: (ProgramDayEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        item(key = "title") {
            AppTextField(
                value = state.title,
                onValueChange = { onEvent(ProgramDayEvent.OnTitleChanged(it)) },
                label = TextFieldLabel.Text(stringResource(Res.string.program_day_title_label)),
            )
        }
        item(key = "picker") {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                AppText(
                    text = stringResource(Res.string.program_day_add_hint),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textMuted,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                    items(items = state.choices, key = { it.exerciseId }) { choice ->
                        AppButton(
                            text = choice.name,
                            onClick = { onEvent(ProgramDayEvent.OnExerciseAdded(choice.exerciseId)) },
                            tone = ButtonTone.Secondary,
                            size = ButtonSize.Small,
                        )
                    }
                }
            }
        }
        itemsIndexed(items = state.lines) { index, line ->
            LineCard(index = index, line = line, onEvent = onEvent)
        }
    }
}

@Composable
private fun LineCard(index: Int, line: ExerciseLineRow, onEvent: (ProgramDayEvent) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    modifier = Modifier.weight(1f),
                    text = line.exerciseName,
                    style = AppTheme.typography.bodyStrong,
                    color = AppTheme.colors.textPrimary,
                )
                AppButton(
                    text = stringResource(Res.string.program_day_remove_action),
                    onClick = { onEvent(ProgramDayEvent.OnLineRemoved(index)) },
                    tone = ButtonTone.Text,
                    size = ButtonSize.Small,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                AppTextField(
                    modifier = Modifier.weight(1f),
                    value = line.setsText,
                    onValueChange = { onEvent(ProgramDayEvent.OnSetsChanged(index, it)) },
                    kind = TextFieldKind.Numeric,
                    label = TextFieldLabel.Text(stringResource(Res.string.program_day_sets_label)),
                )
                AppTextField(
                    modifier = Modifier.weight(1f),
                    value = line.repetitionsText,
                    onValueChange = { onEvent(ProgramDayEvent.OnRepetitionsChanged(index, it)) },
                    kind = TextFieldKind.Numeric,
                    label = TextFieldLabel.Text(stringResource(Res.string.program_day_reps_label)),
                )
                AppTextField(
                    modifier = Modifier.weight(1f),
                    value = line.weightText,
                    onValueChange = { onEvent(ProgramDayEvent.OnWeightChanged(index, it)) },
                    kind = TextFieldKind.Numeric,
                    label = TextFieldLabel.Text(stringResource(Res.string.program_day_weight_label)),
                )
            }
        }
    }
}
