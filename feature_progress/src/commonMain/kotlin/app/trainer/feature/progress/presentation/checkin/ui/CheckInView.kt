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
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.progress.presentation.checkin.mvi.CheckInEvent
import app.trainer.feature.progress.presentation.checkin.mvi.CheckInState
import app.trainer.feature.progress.presentation.checkin.mvi.PhotoRow
import app.trainer.media.rememberImagePicker
import app.trainer.strings.Res
import app.trainer.strings.check_in_adherence_label
import app.trainer.strings.check_in_chest_label
import app.trainer.strings.check_in_hips_label
import app.trainer.strings.check_in_notes_label
import app.trainer.strings.check_in_photos_hint
import app.trainer.strings.check_in_photos_label
import app.trainer.strings.check_in_save_action
import app.trainer.strings.check_in_sleep_label
import app.trainer.strings.check_in_title
import app.trainer.strings.check_in_waist_label
import app.trainer.strings.check_in_weight_label
import app.trainer.strings.check_in_wellbeing_label
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAddPhotoTile
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppPhotoThumb
import app.trainer.uikit.widgets.AppRatingSelector
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PhotoThumbAction
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarLeading
import app.trainer.uikit.widgets.TopBarSubtitle
import org.jetbrains.compose.resources.stringResource

private const val WEIGHT_PLACEHOLDER = "82,4"
private const val PHOTO_TILES_IN_ROW = 3

@Composable
fun CheckInView(
    modifier: Modifier = Modifier,
    state: CheckInState,
    onEvent: (CheckInEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.check_in_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
            subtitle = TopBarSubtitle.Text(state.dateLabel),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (state.failure != null) {
                AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(CheckInEvent.OnRetryClicked) },
                )
            } else {
                CheckInForm(state = state, onEvent = onEvent)
            }
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = stringResource(Res.string.check_in_save_action),
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
            label = TextFieldLabel.Text(stringResource(Res.string.check_in_weight_label)),
            placeholder = WEIGHT_PLACEHOLDER,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppTextField(
                modifier = Modifier.weight(1f),
                value = state.waistText,
                onValueChange = { onEvent(CheckInEvent.OnWaistChanged(it)) },
                kind = TextFieldKind.Numeric,
                label = TextFieldLabel.Text(stringResource(Res.string.check_in_waist_label)),
            )
            AppTextField(
                modifier = Modifier.weight(1f),
                value = state.chestText,
                onValueChange = { onEvent(CheckInEvent.OnChestChanged(it)) },
                kind = TextFieldKind.Numeric,
                label = TextFieldLabel.Text(stringResource(Res.string.check_in_chest_label)),
            )
            AppTextField(
                modifier = Modifier.weight(1f),
                value = state.hipsText,
                onValueChange = { onEvent(CheckInEvent.OnHipsChanged(it)) },
                kind = TextFieldKind.Numeric,
                label = TextFieldLabel.Text(stringResource(Res.string.check_in_hips_label)),
            )
        }
        RatingRow(
            label = stringResource(Res.string.check_in_wellbeing_label),
            selected = state.wellbeing,
            onSelect = { onEvent(CheckInEvent.OnWellbeingSelected(it)) },
        )
        RatingRow(
            label = stringResource(Res.string.check_in_sleep_label),
            selected = state.sleepQuality,
            onSelect = { onEvent(CheckInEvent.OnSleepQualitySelected(it)) },
        )
        RatingRow(
            label = stringResource(Res.string.check_in_adherence_label),
            selected = state.adherence,
            onSelect = { onEvent(CheckInEvent.OnAdherenceSelected(it)) },
        )
        AppTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.notes,
            onValueChange = { onEvent(CheckInEvent.OnNotesChanged(it)) },
            kind = TextFieldKind.Multiline,
            label = TextFieldLabel.Text(stringResource(Res.string.check_in_notes_label)),
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
            text = stringResource(Res.string.check_in_photos_label),
            style = AppTheme.typography.label,
            color = AppTheme.colors.textSecondary,
        )
        AppText(
            text = stringResource(Res.string.check_in_photos_hint),
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
                            contentDescription = stringResource(Res.string.check_in_photos_label),
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
