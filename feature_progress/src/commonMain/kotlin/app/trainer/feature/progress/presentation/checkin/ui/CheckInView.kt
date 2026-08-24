package app.trainer.feature.progress.presentation.checkin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.feature.progress.presentation.checkin.mvi.CheckInEvent
import app.trainer.feature.progress.presentation.checkin.mvi.PhotoRow
import app.trainer.media.rememberImagePicker
import app.trainer.feature.progress.presentation.checkin.mvi.CheckInState
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAddPhotoTile
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppPhotoThumb
import app.trainer.uikit.widgets.AppRatingSelector
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PhotoThumbAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.TopBarSubtitle

private const val TITLE = "Чек-ин"
private const val WEIGHT_LABEL = "Вес, кг"
private const val WEIGHT_PLACEHOLDER = "82,4"
private const val WAIST_LABEL = "Талия, см"
private const val CHEST_LABEL = "Грудь, см"
private const val HIPS_LABEL = "Бёдра, см"
private const val WELLBEING_LABEL = "Самочувствие"
private const val SLEEP_LABEL = "Сон"
private const val NOTES_LABEL = "Что важно сказать тренеру"
private const val PHOTOS_LABEL = "Фото"
private const val PHOTOS_HINT = "Видит только тренер. Снимайте в одном свете и ракурсе."
private const val PHOTO_TILES_IN_ROW = 3
private const val SAVE_ACTION = "Сохранить"
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"

@Composable
fun CheckInView(
    modifier: Modifier = Modifier,
    state: CheckInState,
    onEvent: (CheckInEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = TITLE,
            leading = TopBarLeading.Back(onClick = onBackClick),
            subtitle = TopBarSubtitle.Text(state.dateLabel),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (state.isFailed) {
                AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(CheckInEvent.OnRetryClicked) },
                    ),
                )
            } else {
                CheckInForm(state = state, onEvent = onEvent)
            }
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = SAVE_ACTION,
            onClick = { onEvent(CheckInEvent.OnSaveClicked) },
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
private fun CheckInForm(state: CheckInState, onEvent: (CheckInEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.weightText,
            onValueChange = { onEvent(CheckInEvent.OnWeightChanged(it)) },
            kind = TextFieldKind.Numeric,
            label = TextFieldLabel.Text(WEIGHT_LABEL),
            placeholder = WEIGHT_PLACEHOLDER,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppTextField(
                modifier = Modifier.weight(1f),
                value = state.waistText,
                onValueChange = { onEvent(CheckInEvent.OnWaistChanged(it)) },
                kind = TextFieldKind.Numeric,
                label = TextFieldLabel.Text(WAIST_LABEL),
            )
            AppTextField(
                modifier = Modifier.weight(1f),
                value = state.chestText,
                onValueChange = { onEvent(CheckInEvent.OnChestChanged(it)) },
                kind = TextFieldKind.Numeric,
                label = TextFieldLabel.Text(CHEST_LABEL),
            )
            AppTextField(
                modifier = Modifier.weight(1f),
                value = state.hipsText,
                onValueChange = { onEvent(CheckInEvent.OnHipsChanged(it)) },
                kind = TextFieldKind.Numeric,
                label = TextFieldLabel.Text(HIPS_LABEL),
            )
        }
        RatingRow(
            label = WELLBEING_LABEL,
            selected = state.wellbeing,
            onSelect = { onEvent(CheckInEvent.OnWellbeingSelected(it)) },
        )
        RatingRow(
            label = SLEEP_LABEL,
            selected = state.sleepQuality,
            onSelect = { onEvent(CheckInEvent.OnSleepQualitySelected(it)) },
        )
        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.notes,
            onValueChange = { onEvent(CheckInEvent.OnNotesChanged(it)) },
            kind = TextFieldKind.Multiline,
            label = TextFieldLabel.Text(NOTES_LABEL),
        )
        PhotoSection(state = state, onEvent = onEvent)
    }
}

private sealed interface PhotoTile {

    data class Photo(val row: PhotoRow) : PhotoTile

    data object Add : PhotoTile
}

@Composable
private fun PhotoSection(state: CheckInState, onEvent: (CheckInEvent) -> Unit) {
    val picker = rememberImagePicker { image -> onEvent(CheckInEvent.OnPhotoPicked(image)) }
    val tiles = state.photos.map(PhotoTile::Photo) + if (state.canAddPhoto) listOf(PhotoTile.Add) else emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
        AppText(
            text = PHOTOS_LABEL,
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        AppText(
            text = PHOTOS_HINT,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textMuted,
        )
        tiles.chunked(PHOTO_TILES_IN_ROW).forEach { rowTiles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                rowTiles.forEach { tile ->
                    when (tile) {
                        is PhotoTile.Photo -> AppPhotoThumb(
                            modifier = Modifier.weight(1f),
                            url = tile.row.url,
                            cacheKey = tile.row.photoId,
                            contentDescription = PHOTOS_LABEL,
                            action = PhotoThumbAction.Remove(
                                onClick = { onEvent(CheckInEvent.OnPhotoRemoved(tile.row.photoId)) },
                            ),
                        )
                        PhotoTile.Add -> AppAddPhotoTile(
                            modifier = Modifier.weight(1f),
                            onClick = picker::pick,
                        )
                    }
                }
                repeat(PHOTO_TILES_IN_ROW - rowTiles.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RatingRow(label: String, selected: Int?, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
        AppText(
            text = label,
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        AppRatingSelector(selected = selected, onSelect = onSelect)
    }
}
