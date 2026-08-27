package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder
import app.trainer.uikit.resources.Res
import app.trainer.uikit.resources.photo_add
import app.trainer.uikit.resources.photo_remove
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.jetbrains.compose.resources.stringResource

private const val PHOTO_ASPECT_RATIO = 1f
private val REMOVE_BUTTON_SIZE = 24.dp
private val REMOVE_BUTTON_PADDING = 4.dp

sealed interface PhotoThumbAction {

    data object None : PhotoThumbAction

    data class Remove(val onClick: () -> Unit) : PhotoThumbAction
}

@Composable
fun AppRemoteImage(
    modifier: Modifier = Modifier,
    url: String,
    cacheKey: String,
    contentDescription: String?,
) {
    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(url)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .crossfade(enable = true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun AppPhotoThumb(
    modifier: Modifier = Modifier,
    url: String,
    cacheKey: String,
    contentDescription: String?,
    action: PhotoThumbAction = PhotoThumbAction.None,
) {
    Box(
        modifier = modifier
            .aspectRatio(PHOTO_ASPECT_RATIO)
            .clip(RoundedCornerShape(AppTheme.radius.dp8))
            .background(AppTheme.colors.bgSurfaceSunken),
    ) {
        AppRemoteImage(
            modifier = Modifier.fillMaxSize(),
            url = url,
            cacheKey = cacheKey,
            contentDescription = contentDescription,
        )
        when (action) {
            PhotoThumbAction.None -> Unit
            is PhotoThumbAction.Remove -> Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(REMOVE_BUTTON_PADDING)
                    .size(REMOVE_BUTTON_SIZE)
                    .background(color = AppTheme.colors.bgInverse, shape = CircleShape)
                    .clickable(onClick = action.onClick),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    painter = AppIcons.close,
                    contentDescription = stringResource(Res.string.photo_remove),
                    size = IconSize.Small,
                    tint = AppTheme.colors.textInverse,
                )
            }
        }
    }
}

@Composable
fun AppAddPhotoTile(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(PHOTO_ASPECT_RATIO)
            .dashedBorder(color = AppTheme.colors.borderStrong, cornerRadius = AppTheme.radius.dp8)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            painter = AppIcons.add,
            contentDescription = stringResource(Res.string.photo_add),
            size = IconSize.Medium,
            tint = AppTheme.colors.accent,
        )
    }
}
