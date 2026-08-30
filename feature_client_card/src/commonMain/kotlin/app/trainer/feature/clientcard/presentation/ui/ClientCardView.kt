package app.trainer.feature.clientcard.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.base.metrics.MetricDynamicsCard
import app.trainer.data.clients.ClientNoteKind
import app.trainer.feature.clientcard.presentation.mvi.CheckInPhotoRow
import app.trainer.feature.clientcard.presentation.mvi.CheckInReview
import app.trainer.feature.clientcard.presentation.mvi.ClientCardEvent
import app.trainer.feature.clientcard.presentation.mvi.ClientCardState
import app.trainer.feature.clientcard.presentation.mvi.ClientCardTab
import app.trainer.feature.clientcard.presentation.mvi.ClientProgramState
import app.trainer.feature.clientcard.presentation.mvi.NoteEditor
import app.trainer.feature.clientcard.presentation.mvi.ProgramPicker
import app.trainer.feature.clientcard.presentation.mvi.ProgramStart
import app.trainer.feature.clientcard.presentation.mvi.ReviewEditor
import app.trainer.strings.Res
import app.trainer.strings.client_card_add_action
import app.trainer.strings.client_card_add_habit_action
import app.trainer.strings.client_card_archive_action
import app.trainer.strings.client_card_archive_cancel
import app.trainer.strings.client_card_archive_confirm
import app.trainer.strings.client_card_archive_description
import app.trainer.strings.client_card_archive_title
import app.trainer.strings.client_card_check_in_photo_description
import app.trainer.strings.client_card_check_ins_empty
import app.trainer.strings.client_card_check_ins_section
import app.trainer.strings.client_card_details_label
import app.trainer.strings.client_card_editor_title
import app.trainer.strings.client_card_empty_action
import app.trainer.strings.client_card_empty_description
import app.trainer.strings.client_card_empty_title
import app.trainer.strings.client_card_general_toggle
import app.trainer.strings.client_card_habits_empty
import app.trainer.strings.client_card_habits_section
import app.trainer.strings.client_card_history_hint
import app.trainer.strings.client_card_history_section
import app.trainer.strings.client_card_medical_toggle
import app.trainer.strings.client_card_new_habit_label
import app.trainer.strings.client_card_notes_section
import app.trainer.strings.client_card_notes_section_empty
import app.trainer.strings.client_card_open_diary_action
import app.trainer.strings.client_card_photos_action
import app.trainer.strings.client_card_pin_action
import app.trainer.strings.client_card_program_assign
import app.trainer.strings.client_card_program_change
import app.trainer.strings.client_card_program_none
import app.trainer.strings.client_card_program_picker_dismiss
import app.trainer.strings.client_card_program_picker_empty
import app.trainer.strings.client_card_program_picker_more
import app.trainer.strings.client_card_program_picker_title
import app.trainer.strings.client_card_program_remove
import app.trainer.strings.client_card_program_section
import app.trainer.strings.client_card_program_start_monday
import app.trainer.strings.client_card_program_start_today
import app.trainer.strings.client_card_review_action
import app.trainer.strings.client_card_review_awaiting
import app.trainer.strings.client_card_review_dismiss
import app.trainer.strings.client_card_review_edit_action
import app.trainer.strings.client_card_review_label
import app.trainer.strings.client_card_review_save
import app.trainer.strings.client_card_review_title
import app.trainer.strings.client_card_save_action
import app.trainer.strings.client_card_tab_history
import app.trainer.strings.client_card_tab_metrics
import app.trainer.strings.client_card_tab_now
import app.trainer.strings.client_card_title
import app.trainer.strings.client_card_title_label
import app.trainer.strings.client_card_unpin_action
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppBottomSheetContainer
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppNoteCard
import app.trainer.uikit.widgets.AppPhotoThumb
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppTabs
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.ConfirmDialogDismiss
import app.trainer.uikit.widgets.ConfirmDialogTone
import app.trainer.uikit.widgets.NoteDetails
import app.trainer.uikit.widgets.NoteKindView
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarAction
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val CHECK_IN_PHOTOS_IN_ROW = 3
private const val SHIMMER_CARDS = 4
private const val SHIMMER_CARD_LINES = 3

@Composable
fun ClientCardView(
    modifier: Modifier = Modifier,
    state: ClientCardState,
    onEvent: (ClientCardEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.client_card_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
            action = TopBarAction.Icon(
                painter = { AppIcons.add },
                contentDescription = stringResource(Res.string.client_card_add_action),
                onClick = { onEvent(ClientCardEvent.OnAddNoteClicked) },
            ),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(ClientCardEvent.OnRetryClicked) },
                )
                state.isLoading -> AppCardShimmerList(
                    count = SHIMMER_CARDS,
                    lines = SHIMMER_CARD_LINES,
                )
                state.isEmptyCard -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.client_card_empty_title),
                    description = stringResource(Res.string.client_card_empty_description),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.client_card_empty_action),
                        onClick = { onEvent(ClientCardEvent.OnAddNoteClicked) },
                    ),
                )
                else -> CardContent(state = state, onEvent = onEvent)
            }
        }
        if (!state.isLoading && state.failure == null) {
            ArchiveFooter(state = state, onEvent = onEvent)
        }
        state.editor?.let { editor ->
            NoteEditorSheet(editor = editor, onEvent = onEvent)
        }
        state.programPicker?.let { picker ->
            ProgramPickerSheet(picker = picker, onEvent = onEvent)
        }
        state.reviewEditor?.let { editor ->
            ReviewSheet(editor = editor, onEvent = onEvent)
        }
    }
    if (state.isArchiveDialogVisible) {
        AppConfirmDialog(
            title = stringResource(Res.string.client_card_archive_title),
            description = stringResource(Res.string.client_card_archive_description),
            confirmText = stringResource(Res.string.client_card_archive_confirm),
            onConfirm = { onEvent(ClientCardEvent.OnArchiveConfirmed) },
            onDismissRequest = { onEvent(ClientCardEvent.OnArchiveDismissed) },
            tone = ConfirmDialogTone.Danger,
            dismiss = ConfirmDialogDismiss.Action(
                text = stringResource(Res.string.client_card_archive_cancel),
                onClick = { onEvent(ClientCardEvent.OnArchiveDismissed) },
            ),
        )
    }
}

@Composable
private fun ArchiveFooter(state: ClientCardState, onEvent: (ClientCardEvent) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(AppTheme.spacing.dp16),
    ) {
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.client_card_archive_action),
            onClick = { onEvent(ClientCardEvent.OnArchiveClientClicked) },
            tone = ButtonTone.Danger,
            size = ButtonSize.Large,
            state = if (state.isArchiving) ButtonState.Loading else ButtonState.Idle,
        )
    }
}

@Composable
private fun CardContent(state: ClientCardState, onEvent: (ClientCardEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTabs(
            tabs = ClientCardTab.entries,
            selected = state.tab,
            labelOf = { tab ->
                when (tab) {
                    ClientCardTab.Now -> stringResource(Res.string.client_card_tab_now)
                    ClientCardTab.Metrics -> stringResource(Res.string.client_card_tab_metrics)
                    ClientCardTab.History -> stringResource(Res.string.client_card_tab_history)
                }
            },
            onSelect = { onEvent(ClientCardEvent.OnTabSelected(it)) },
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(AppTheme.spacing.dp16),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            medicalNotes(state = state, onEvent = onEvent)
            when (state.tab) {
                ClientCardTab.Now -> nowTab(state = state, onEvent = onEvent)
                ClientCardTab.Metrics -> metricsTab(state = state, onEvent = onEvent)
                ClientCardTab.History -> historyTab(onEvent = onEvent)
            }
        }
    }
}

private fun LazyListScope.medicalNotes(state: ClientCardState, onEvent: (ClientCardEvent) -> Unit) {
    val medical = state.notes.filter { it.kind == ClientNoteKind.MEDICAL }
    if (medical.isEmpty()) return
    items(items = medical, key = { "medical-${it.noteId}" }) { note ->
        AppNoteCard(
            modifier = Modifier.animateItem(),
            title = note.title,
            kind = NoteKindView.Medical,
            isPinned = true,
            updatedAt = note.updatedAtLabel,
            onClick = { onEvent(ClientCardEvent.OnNoteClicked(note.noteId)) },
            details = note.details?.let(NoteDetails::Text) ?: NoteDetails.None,
        )
    }
}

private fun LazyListScope.nowTab(state: ClientCardState, onEvent: (ClientCardEvent) -> Unit) {
    item(key = "program") { ProgramCard(state = state, onEvent = onEvent) }
    item(key = "notes-title") { SectionTitle(text = stringResource(Res.string.client_card_notes_section)) }
    val general = state.notes.filterNot { it.kind == ClientNoteKind.MEDICAL }
    if (general.isEmpty()) {
        item(key = "notes-empty") { SectionHint(text = stringResource(Res.string.client_card_notes_section_empty)) }
    }
    items(items = general, key = { it.noteId }) { note ->
        AppNoteCard(
            modifier = Modifier.animateItem(),
            title = note.title,
            kind = NoteKindView.General,
            isPinned = note.isPinned,
            updatedAt = note.updatedAtLabel,
            onClick = { onEvent(ClientCardEvent.OnNoteClicked(note.noteId)) },
            details = note.details?.let(NoteDetails::Text) ?: NoteDetails.None,
        )
    }
    item(key = "habits-title") { SectionTitle(text = stringResource(Res.string.client_card_habits_section)) }
    if (state.habits.isEmpty()) {
        item(key = "habits-empty") { SectionHint(text = stringResource(Res.string.client_card_habits_empty)) }
    }
    items(items = state.habits, key = { it.habitId }) { habit ->
        HabitCard(
            modifier = Modifier.animateItem(),
            title = habit.title,
            doneCountLabel = habit.doneCountLabel,
        )
    }
    item(key = "new-habit") { NewHabitRow(state = state, onEvent = onEvent) }
}

private fun LazyListScope.metricsTab(state: ClientCardState, onEvent: (ClientCardEvent) -> Unit) {
    state.selectedChart?.let { chart ->
        item(key = "dynamics") {
            MetricDynamicsCard(
                charts = state.charts,
                chart = chart,
                onMetricClick = { onEvent(ClientCardEvent.OnMetricSelected(it)) },
            )
        }
    }
    item(key = "photos-compare") {
        AppButton(
            text = stringResource(Res.string.client_card_photos_action),
            onClick = { onEvent(ClientCardEvent.OnComparePhotosClicked) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Small,
        )
    }
    item(key = "check-ins-title") { SectionTitle(text = stringResource(Res.string.client_card_check_ins_section)) }
    if (state.checkIns.isEmpty()) {
        item(key = "check-ins-empty") { SectionHint(text = stringResource(Res.string.client_card_check_ins_empty)) }
    }
    items(items = state.checkIns, key = { it.checkInId }) { checkIn ->
        CheckInCard(
            modifier = Modifier.animateItem(),
            dateLabel = checkIn.dateLabel,
            measurements = checkIn.measurements,
            wellbeing = checkIn.wellbeingLabel,
            notes = checkIn.notes,
            photos = checkIn.photos,
            review = checkIn.review,
            onReviewClick = { onEvent(ClientCardEvent.OnReviewClicked(checkIn.checkInId)) },
        )
    }
}

private fun LazyListScope.historyTab(onEvent: (ClientCardEvent) -> Unit) {
    item(key = "history-title") { SectionTitle(text = stringResource(Res.string.client_card_history_section)) }
    item(key = "history-hint") { SectionHint(text = stringResource(Res.string.client_card_history_hint)) }
    item(key = "open-diary") {
        AppButton(
            text = stringResource(Res.string.client_card_open_diary_action),
            onClick = { onEvent(ClientCardEvent.OnOpenDiaryClicked) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Small,
        )
    }
}

@Composable
private fun ReviewBlock(review: CheckInReview, onReviewClick: () -> Unit) {
    when (review) {
        CheckInReview.Awaiting -> Row(
            modifier = Modifier.fillMaxWidth().padding(top = AppTheme.spacing.dp4),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.client_card_review_awaiting),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.warning,
            )
            AppButton(
                text = stringResource(Res.string.client_card_review_action),
                onClick = onReviewClick,
                tone = ButtonTone.Primary,
                size = ButtonSize.Small,
            )
        }
        is CheckInReview.Answered -> Column(
            modifier = Modifier.padding(top = AppTheme.spacing.dp4),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
        ) {
            review.comment?.let { comment ->
                AppText(
                    text = comment,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
            }
            AppButton(
                text = stringResource(Res.string.client_card_review_edit_action),
                onClick = onReviewClick,
                tone = ButtonTone.Text,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun ReviewSheet(editor: ReviewEditor, onEvent: (ClientCardEvent) -> Unit) {
    AppBottomSheetContainer(title = stringResource(Res.string.client_card_review_title)) {
        AppTextField(
            value = editor.comment,
            onValueChange = { onEvent(ClientCardEvent.OnReviewCommentChanged(it)) },
            kind = TextFieldKind.Multiline,
            label = TextFieldLabel.Text(stringResource(Res.string.client_card_review_label)),
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.client_card_review_save),
            onClick = { onEvent(ClientCardEvent.OnReviewSaveClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = if (editor.isSaving) ButtonState.Loading else ButtonState.Idle,
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.client_card_review_dismiss),
            onClick = { onEvent(ClientCardEvent.OnReviewDismissed) },
            tone = ButtonTone.Text,
            size = ButtonSize.Large,
        )
    }
}

@Composable
private fun ProgramCard(state: ClientCardState, onEvent: (ClientCardEvent) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppText(
                text = stringResource(Res.string.client_card_program_section),
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
            )
            when (val program = state.program) {
                ClientProgramState.None -> {
                    AppText(
                        text = stringResource(Res.string.client_card_program_none),
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textMuted,
                    )
                    AppButton(
                        text = stringResource(Res.string.client_card_program_assign),
                        onClick = { onEvent(ClientCardEvent.OnAssignProgramClicked) },
                        tone = ButtonTone.Primary,
                        size = ButtonSize.Medium,
                    )
                }
                is ClientProgramState.Assigned -> {
                    AppText(
                        text = program.title,
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = program.startsLabel,
                        style = AppTheme.typography.numeric,
                        color = AppTheme.colors.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                        AppButton(
                            text = stringResource(Res.string.client_card_program_change),
                            onClick = { onEvent(ClientCardEvent.OnAssignProgramClicked) },
                            tone = ButtonTone.Secondary,
                            size = ButtonSize.Small,
                        )
                        AppButton(
                            text = stringResource(Res.string.client_card_program_remove),
                            onClick = { onEvent(ClientCardEvent.OnProgramRemoved) },
                            tone = ButtonTone.Text,
                            size = ButtonSize.Small,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramPickerSheet(picker: ProgramPicker, onEvent: (ClientCardEvent) -> Unit) {
    AppBottomSheetContainer(title = stringResource(Res.string.client_card_program_picker_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppButton(
                text = stringResource(Res.string.client_card_program_start_today),
                onClick = { onEvent(ClientCardEvent.OnProgramStartSelected(ProgramStart.Today)) },
                tone = if (picker.startsOn == ProgramStart.Today) ButtonTone.Primary else ButtonTone.Secondary,
                size = ButtonSize.Small,
            )
            AppButton(
                text = stringResource(Res.string.client_card_program_start_monday),
                onClick = { onEvent(ClientCardEvent.OnProgramStartSelected(ProgramStart.NextMonday)) },
                tone = if (picker.startsOn == ProgramStart.NextMonday) {
                    ButtonTone.Primary
                } else {
                    ButtonTone.Secondary
                },
                size = ButtonSize.Small,
            )
        }
        if (picker.programs.isEmpty() && !picker.isLoading) {
            AppText(
                text = stringResource(Res.string.client_card_program_picker_empty),
                style = AppTheme.typography.body,
                color = AppTheme.colors.textMuted,
            )
        }
        picker.programs.forEach { row ->
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = row.title,
                onClick = { onEvent(ClientCardEvent.OnProgramPicked(row.programId)) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Large,
                state = if (picker.isSaving) ButtonState.Disabled else ButtonState.Idle,
            )
        }
        if (picker.hasMore) {
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.client_card_program_picker_more),
                onClick = { onEvent(ClientCardEvent.OnProgramPickerMoreClicked) },
                tone = ButtonTone.Text,
                size = ButtonSize.Large,
                state = if (picker.isLoadingMore) ButtonState.Disabled else ButtonState.Idle,
            )
        }
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.client_card_program_picker_dismiss),
            onClick = { onEvent(ClientCardEvent.OnProgramPickerDismissed) },
            tone = ButtonTone.Text,
            size = ButtonSize.Large,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    AppText(
        modifier = Modifier.padding(top = AppTheme.spacing.dp8),
        text = text,
        style = AppTheme.typography.headline,
        color = AppTheme.colors.textPrimary,
    )
}

@Composable
private fun SectionHint(text: String) {
    AppText(
        text = text,
        style = AppTheme.typography.body,
        color = AppTheme.colors.textSecondary,
    )
}

@Composable
private fun CheckInCard(
    modifier: Modifier = Modifier,
    dateLabel: String,
    measurements: String,
    wellbeing: String,
    notes: String?,
    photos: List<CheckInPhotoRow>,
    review: CheckInReview,
    onReviewClick: () -> Unit,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            AppText(
                text = dateLabel,
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
            )
            AppText(
                text = measurements,
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = wellbeing,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            notes?.let { text ->
                AppText(
                    text = text,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
            }
            ReviewBlock(review = review, onReviewClick = onReviewClick)
            if (photos.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = AppTheme.spacing.dp4),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                ) {
                    photos.forEach { photo ->
                        AppPhotoThumb(
                            modifier = Modifier.weight(1f),
                            url = photo.url,
                            cacheKey = photo.photoId,
                            contentDescription = stringResource(Res.string.client_card_check_in_photo_description),
                        )
                    }
                    repeat(CHECK_IN_PHOTOS_IN_ROW - photos.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCard(modifier: Modifier = Modifier, title: String, doneCountLabel: String) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                modifier = Modifier.weight(1f),
                text = title,
                style = AppTheme.typography.body,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = doneCountLabel,
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
            )
        }
    }
}

@Composable
private fun NewHabitRow(state: ClientCardState, onEvent: (ClientCardEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.Bottom,
    ) {
        AppTextField(
            modifier = Modifier.weight(1f),
            value = state.newHabitTitle,
            onValueChange = { onEvent(ClientCardEvent.OnNewHabitTitleChanged(it)) },
            kind = TextFieldKind.Text,
            label = TextFieldLabel.Text(stringResource(Res.string.client_card_new_habit_label)),
        )
        AppButton(
            text = stringResource(Res.string.client_card_add_habit_action),
            onClick = { onEvent(ClientCardEvent.OnHabitAdded) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Large,
            state = if (state.isAddHabitEnabled) ButtonState.Idle else ButtonState.Disabled,
        )
    }
}

@Composable
private fun NoteEditorSheet(editor: NoteEditor, onEvent: (ClientCardEvent) -> Unit) {
    AppBottomSheetContainer(title = stringResource(Res.string.client_card_editor_title)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            AppButton(
                text = stringResource(Res.string.client_card_medical_toggle),
                onClick = { onEvent(ClientCardEvent.OnEditorKindChanged(ClientNoteKind.MEDICAL)) },
                tone = if (editor.kind == ClientNoteKind.MEDICAL) ButtonTone.Danger else ButtonTone.Secondary,
                size = ButtonSize.Small,
            )
            AppButton(
                text = stringResource(Res.string.client_card_general_toggle),
                onClick = { onEvent(ClientCardEvent.OnEditorKindChanged(ClientNoteKind.GENERAL)) },
                tone = if (editor.kind == ClientNoteKind.GENERAL) ButtonTone.Primary else ButtonTone.Secondary,
                size = ButtonSize.Small,
            )
        }
        AppTextField(
            value = editor.title,
            onValueChange = { onEvent(ClientCardEvent.OnEditorTitleChanged(it)) },
            label = TextFieldLabel.Text(stringResource(Res.string.client_card_title_label)),
        )
        AppTextField(
            value = editor.details,
            onValueChange = { onEvent(ClientCardEvent.OnEditorDetailsChanged(it)) },
            kind = TextFieldKind.Multiline,
            label = TextFieldLabel.Text(stringResource(Res.string.client_card_details_label)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            AppButton(
                modifier = Modifier.weight(1f),
                text = if (editor.isPinned) {
                    stringResource(Res.string.client_card_unpin_action)
                } else {
                    stringResource(Res.string.client_card_pin_action)
                },
                onClick = { onEvent(ClientCardEvent.OnEditorPinToggled) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Large,
            )
            AppButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.client_card_save_action),
                onClick = { onEvent(ClientCardEvent.OnEditorSaveClicked) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Large,
                state = when {
                    editor.isSaving -> ButtonState.Loading
                    !editor.isSaveEnabled -> ButtonState.Disabled
                    else -> ButtonState.Idle
                },
            )
        }
    }
}
