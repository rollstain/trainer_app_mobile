package app.trainer.feature.traininglog.presentation.programs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.traininglog.presentation.programs.mvi.NewProgramDraft
import app.trainer.feature.traininglog.presentation.programs.mvi.ProgramsEvent
import app.trainer.feature.traininglog.presentation.programs.mvi.ProgramsState
import app.trainer.strings.Res
import app.trainer.strings.programs_archive_action
import app.trainer.strings.programs_create_action
import app.trainer.strings.programs_draft_dismiss
import app.trainer.strings.programs_draft_save
import app.trainer.strings.programs_draft_title
import app.trainer.strings.programs_draft_title_label
import app.trainer.strings.programs_duplicate_action
import app.trainer.strings.programs_empty_description
import app.trainer.strings.programs_empty_title
import app.trainer.strings.programs_title
import app.trainer.strings.programs_weeks_label
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppBottomSheetContainer
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardAction
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val LOAD_MORE_CARDS = 1
private const val SHIMMER_CARD_LINES = 2

@Composable
fun ProgramsView(
    state: ProgramsState,
    onEvent: (ProgramsEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.programs_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ProgramsEvent.OnRetryClicked) },
                )
                state.programs.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.programs_empty_title),
                    description = stringResource(Res.string.programs_empty_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.programs_create_action),
                        onClick = { onEvent(ProgramsEvent.OnCreateClicked) },
                    ),
                )
                else -> ProgramList(state = state, onEvent = onEvent)
            }
        }
        if (state.programs.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(AppTheme.spacing.dp16),
            ) {
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.programs_create_action),
                    onClick = { onEvent(ProgramsEvent.OnCreateClicked) },
                    tone = ButtonTone.Primary,
                    size = ButtonSize.Large,
                )
            }
        }
        state.draft?.let { draft ->
            DraftSheet(draft = draft, onEvent = onEvent)
        }
    }
}

@Composable
private fun ProgramList(state: ProgramsState, onEvent: (ProgramsEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        items(items = state.programs, key = { it.programId }) { program ->
            AppCard(
                action = CardAction.Click(
                    onClick = { onEvent(ProgramsEvent.OnProgramClicked(program.programId)) }
                ),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                    AppText(
                        text = program.title,
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = program.summary,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                        AppButton(
                            text = stringResource(Res.string.programs_duplicate_action),
                            onClick = { onEvent(ProgramsEvent.OnProgramDuplicated(program.programId)) },
                            tone = ButtonTone.Secondary,
                            size = ButtonSize.Small,
                        )
                        AppButton(
                            text = stringResource(Res.string.programs_archive_action),
                            onClick = { onEvent(ProgramsEvent.OnProgramArchived(program.programId)) },
                            tone = ButtonTone.Text,
                            size = ButtonSize.Small,
                        )
                    }
                }
            }
        }
        if (state.hasMore) {
            item(key = "load-more") {
                LaunchedEffect(state.nextCursor) { onEvent(ProgramsEvent.OnEndReached) }
                AppCardShimmerList(count = LOAD_MORE_CARDS, lines = SHIMMER_CARD_LINES)
            }
        }
    }
}

@Composable
private fun DraftSheet(draft: NewProgramDraft, onEvent: (ProgramsEvent) -> Unit) {
    AppBottomSheetContainer(title = stringResource(Res.string.programs_draft_title)) {
        AppTextField(
            value = draft.title,
            onValueChange = { onEvent(ProgramsEvent.OnDraftTitleChanged(it)) },
            label = TextFieldLabel.Text(stringResource(Res.string.programs_draft_title_label)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.programs_weeks_label, draft.weeksCount),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
            )
            AppButton(
                text = "−",
                onClick = { onEvent(ProgramsEvent.OnWeekRemoved) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Small,
                state = if (draft.canRemoveWeek) ButtonState.Idle else ButtonState.Disabled,
            )
            AppButton(
                text = "+",
                onClick = { onEvent(ProgramsEvent.OnWeekAdded) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Small,
                state = if (draft.canAddWeek) ButtonState.Idle else ButtonState.Disabled,
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.programs_draft_save),
            onClick = { onEvent(ProgramsEvent.OnDraftSaveClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = when {
                draft.isSaving -> ButtonState.Loading
                draft.isSaveEnabled -> ButtonState.Idle
                else -> ButtonState.Disabled
            },
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.programs_draft_dismiss),
            onClick = { onEvent(ProgramsEvent.OnDraftDismissed) },
            tone = ButtonTone.Text,
            size = ButtonSize.Large,
        )
    }
}
