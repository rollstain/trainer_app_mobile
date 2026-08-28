package app.trainer.feature.progress.presentation.photos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.progress.presentation.photos.mvi.CompareSide
import app.trainer.feature.progress.presentation.photos.mvi.PhotoCompareEvent
import app.trainer.feature.progress.presentation.photos.mvi.PhotoCompareState
import app.trainer.feature.progress.presentation.photos.mvi.PhotoShot
import app.trainer.strings.Res
import app.trainer.strings.photo_compare_after
import app.trainer.strings.photo_compare_before
import app.trainer.strings.photo_compare_empty_description
import app.trainer.strings.photo_compare_empty_title
import app.trainer.strings.photo_compare_pick_hint
import app.trainer.strings.photo_compare_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppRemoteImage
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 2
private const val SHIMMER_CARD_LINES = 3
private const val PHOTO_ASPECT_RATIO = 0.75f
private val THUMB_SIZE = 64.dp

@Composable
fun PhotoCompareView(
    modifier: Modifier = Modifier,
    state: PhotoCompareState,
    onEvent: (PhotoCompareEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.photo_compare_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(PhotoCompareEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                state.canCompare -> Comparison(state = state, onEvent = onEvent)
                else -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.photo_compare_empty_title),
                    description = stringResource(Res.string.photo_compare_empty_description),
                    action = PlaceholderAction.None,
                )
            }
        }
    }
}

@Composable
private fun Comparison(state: PhotoCompareState, onEvent: (PhotoCompareEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            ShotPane(
                modifier = Modifier.weight(1f),
                caption = stringResource(Res.string.photo_compare_before),
                shot = state.before,
                isSelected = state.selectedSide == CompareSide.Before,
                onClick = { onEvent(PhotoCompareEvent.OnSideSelected(CompareSide.Before)) },
            )
            ShotPane(
                modifier = Modifier.weight(1f),
                caption = stringResource(Res.string.photo_compare_after),
                shot = state.after,
                isSelected = state.selectedSide == CompareSide.After,
                onClick = { onEvent(PhotoCompareEvent.OnSideSelected(CompareSide.After)) },
            )
        }
        AppText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.photo_compare_pick_hint),
            style = AppTheme.typography.caption,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            items(items = state.shots, key = { it.photoId }) { shot ->
                Thumb(
                    shot = shot,
                    isPicked = shot.photoId == state.beforePhotoId || shot.photoId == state.afterPhotoId,
                    onClick = { onEvent(PhotoCompareEvent.OnShotPicked(shot.photoId)) },
                )
            }
        }
    }
}

@Composable
private fun ShotPane(
    modifier: Modifier = Modifier,
    caption: String,
    shot: PhotoShot?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppTheme.radius.dp12)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppButton(
            text = caption,
            onClick = onClick,
            tone = if (isSelected) ButtonTone.Primary else ButtonTone.Secondary,
            size = ButtonSize.Small,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PHOTO_ASPECT_RATIO)
                .clip(shape)
                .background(AppTheme.colors.bgSurfaceSunken)
                .border(
                    width = if (isSelected) AppTheme.borders.focus else AppTheme.borders.hairline,
                    color = if (isSelected) AppTheme.colors.accent else AppTheme.colors.border,
                    shape = shape,
                )
                .clickable(onClick = onClick),
        ) {
            if (shot != null) {
                AppRemoteImage(
                    modifier = Modifier.fillMaxSize(),
                    url = shot.url,
                    cacheKey = shot.photoId,
                    contentDescription = shot.dateLabel,
                )
            }
        }
        AppText(
            text = shot?.dateLabel.orEmpty(),
            style = AppTheme.typography.bodyStrong,
            color = AppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun Thumb(shot: PhotoShot, isPicked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppTheme.radius.dp8)
    Box(
        modifier = Modifier
            .size(THUMB_SIZE)
            .clip(shape)
            .background(AppTheme.colors.bgSurfaceSunken)
            .border(
                width = if (isPicked) AppTheme.borders.focus else AppTheme.borders.hairline,
                color = if (isPicked) AppTheme.colors.accent else AppTheme.colors.border,
                shape = shape,
            )
            .clickable(onClick = onClick),
    ) {
        AppRemoteImage(
            modifier = Modifier.fillMaxSize(),
            url = shot.url,
            cacheKey = shot.photoId,
            contentDescription = shot.dateLabel,
        )
    }
}
