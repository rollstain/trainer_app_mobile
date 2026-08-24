package app.trainer.feature.clientcard.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.data.clients.ClientNoteKind
import app.trainer.feature.clientcard.presentation.mvi.CheckInPhotoRow
import app.trainer.feature.clientcard.presentation.mvi.ClientCardEvent
import app.trainer.feature.clientcard.presentation.mvi.ClientCardState
import app.trainer.feature.clientcard.presentation.mvi.NoteEditor
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppBottomSheetContainer
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppNoteCard
import app.trainer.uikit.widgets.AppPhotoThumb
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.NoteDetails
import app.trainer.uikit.widgets.NoteKindView
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarAction
import app.trainer.uikit.widgets.TopBarLeading

private const val TITLE = "Карточка"
private const val ADD_ACTION = "Добавить"
private const val EMPTY_TITLE = "Пометок пока нет"
private const val EMPTY_DESCRIPTION =
    "Запишите важное: ограничения по здоровью, цели, предпочтения. Клиент их не увидит."
private const val EMPTY_ACTION = "Добавить пометку"
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"
private const val EDITOR_TITLE = "Пометка"
private const val TITLE_LABEL = "Что важно помнить"
private const val DETAILS_LABEL = "Подробности"
private const val SAVE_ACTION = "Сохранить"
private const val MEDICAL_TOGGLE = "Медицинская"
private const val GENERAL_TOGGLE = "Обычная"
private const val PIN_ACTION = "Закрепить"
private const val UNPIN_ACTION = "Открепить"
private const val NOTES_SECTION = "Пометки"
private const val NOTES_SECTION_EMPTY =
    "Пометок пока нет. Запишите важное: ограничения по здоровью, цели, предпочтения."
private const val CHECK_INS_SECTION = "Чек-ины"
private const val CHECK_INS_EMPTY = "Подопечный ещё не присылал замеры."
private const val HABITS_SECTION = "Привычки"
private const val HABITS_EMPTY = "Привычек пока нет. Добавьте то, что важно держать под контролем."
private const val NEW_HABIT_LABEL = "Новая привычка"
private const val ADD_HABIT_ACTION = "Добавить"
private const val CHECK_IN_PHOTO_DESCRIPTION = "Фото чек-ина"
private const val CHECK_IN_PHOTOS_IN_ROW = 3

@Composable
fun ClientCardView(
    modifier: Modifier = Modifier,
    state: ClientCardState,
    onEvent: (ClientCardEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = TITLE,
            leading = TopBarLeading.Back(onClick = onBackClick),
            action = TopBarAction.Content(
                onClick = { onEvent(ClientCardEvent.OnAddNoteClicked) },
                render = {
                    AppText(
                        text = ADD_ACTION,
                        style = AppTheme.typography.label,
                        color = AppTheme.colors.accent,
                    )
                },
            ),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isFailed -> AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(ClientCardEvent.OnRetryClicked) },
                    ),
                )
                state.isEmptyCard && !state.isLoading -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EMPTY_TITLE,
                    description = EMPTY_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = EMPTY_ACTION,
                        onClick = { onEvent(ClientCardEvent.OnAddNoteClicked) },
                    ),
                )
                else -> CardContent(state = state, onEvent = onEvent)
            }
        }
        state.editor?.let { editor ->
            NoteEditorSheet(editor = editor, onEvent = onEvent)
        }
    }
}

@Composable
private fun CardContent(state: ClientCardState, onEvent: (ClientCardEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        item(key = "notes-title") { SectionTitle(text = NOTES_SECTION) }
        if (state.notes.isEmpty()) {
            item(key = "notes-empty") { SectionHint(text = NOTES_SECTION_EMPTY) }
        }
        items(items = state.notes, key = { it.noteId }) { note ->
            AppNoteCard(
                title = note.title,
                kind = when (note.kind) {
                    ClientNoteKind.MEDICAL -> NoteKindView.Medical
                    ClientNoteKind.GENERAL -> NoteKindView.General
                },
                isPinned = note.isPinned,
                updatedAt = note.updatedAtLabel,
                onClick = { onEvent(ClientCardEvent.OnNoteClicked(note.noteId)) },
                details = note.details?.let(NoteDetails::Text) ?: NoteDetails.None,
            )
        }
        item(key = "check-ins-title") { SectionTitle(text = CHECK_INS_SECTION) }
        if (state.checkIns.isEmpty()) {
            item(key = "check-ins-empty") { SectionHint(text = CHECK_INS_EMPTY) }
        }
        items(items = state.checkIns, key = { it.checkInId }) { checkIn ->
            CheckInCard(
                dateLabel = checkIn.dateLabel,
                measurements = checkIn.measurements,
                wellbeing = checkIn.wellbeingLabel,
                notes = checkIn.notes,
                photos = checkIn.photos,
            )
        }
        item(key = "habits-title") { SectionTitle(text = HABITS_SECTION) }
        if (state.habits.isEmpty()) {
            item(key = "habits-empty") { SectionHint(text = HABITS_EMPTY) }
        }
        items(items = state.habits, key = { it.habitId }) { habit ->
            HabitCard(title = habit.title, doneCountLabel = habit.doneCountLabel)
        }
        item(key = "new-habit") { NewHabitRow(state = state, onEvent = onEvent) }
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
    dateLabel: String,
    measurements: String,
    wellbeing: String,
    notes: String?,
    photos: List<CheckInPhotoRow>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
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
                        contentDescription = CHECK_IN_PHOTO_DESCRIPTION,
                    )
                }
                repeat(CHECK_IN_PHOTOS_IN_ROW - photos.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HabitCard(title: String, doneCountLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(AppTheme.spacing.dp16),
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
            label = TextFieldLabel.Text(NEW_HABIT_LABEL),
        )
        AppButton(
            text = ADD_HABIT_ACTION,
            onClick = { onEvent(ClientCardEvent.OnHabitAdded) },
            tone = ButtonTone.Secondary,
            size = ButtonSize.Large,
            state = if (state.isAddHabitEnabled) ButtonState.Idle else ButtonState.Disabled,
        )
    }
}

@Composable
private fun NoteEditorSheet(editor: NoteEditor, onEvent: (ClientCardEvent) -> Unit) {
    AppBottomSheetContainer(title = EDITOR_TITLE) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            AppButton(
                text = MEDICAL_TOGGLE,
                onClick = { onEvent(ClientCardEvent.OnEditorKindChanged(ClientNoteKind.MEDICAL)) },
                tone = if (editor.kind == ClientNoteKind.MEDICAL) ButtonTone.Danger else ButtonTone.Secondary,
                size = ButtonSize.Small,
            )
            AppButton(
                text = GENERAL_TOGGLE,
                onClick = { onEvent(ClientCardEvent.OnEditorKindChanged(ClientNoteKind.GENERAL)) },
                tone = if (editor.kind == ClientNoteKind.GENERAL) ButtonTone.Primary else ButtonTone.Secondary,
                size = ButtonSize.Small,
            )
        }
        AppTextField(
            value = editor.title,
            onValueChange = { onEvent(ClientCardEvent.OnEditorTitleChanged(it)) },
            label = TextFieldLabel.Text(TITLE_LABEL),
        )
        AppTextField(
            value = editor.details,
            onValueChange = { onEvent(ClientCardEvent.OnEditorDetailsChanged(it)) },
            kind = TextFieldKind.Multiline,
            label = TextFieldLabel.Text(DETAILS_LABEL),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        ) {
            AppButton(
                modifier = Modifier.weight(1f),
                text = if (editor.isPinned) UNPIN_ACTION else PIN_ACTION,
                onClick = { onEvent(ClientCardEvent.OnEditorPinToggled) },
                tone = ButtonTone.Secondary,
                size = ButtonSize.Large,
            )
            AppButton(
                modifier = Modifier.weight(1f),
                text = SAVE_ACTION,
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
