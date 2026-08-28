package app.trainer.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.trainer.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private const val LOG_TAG = "media-picker"
private const val FALLBACK_IMAGE_NAME = "photo.jpg"
private const val FALLBACK_IMAGE_TYPE = "image/jpeg"
private const val FALLBACK_VIDEO_NAME = "video.mp4"
private const val FALLBACK_VIDEO_TYPE = "video/mp4"

@Composable
actual fun rememberImagePicker(onPicked: (PickedMedia) -> Unit): MediaPicker = rememberPicker(
    request = ActivityResultContracts.PickVisualMedia.ImageOnly,
    fallbackName = FALLBACK_IMAGE_NAME,
    fallbackType = FALLBACK_IMAGE_TYPE,
    onPicked = onPicked,
)

@Composable
actual fun rememberVideoPicker(onPicked: (PickedMedia) -> Unit): MediaPicker = rememberPicker(
    request = ActivityResultContracts.PickVisualMedia.VideoOnly,
    fallbackName = FALLBACK_VIDEO_NAME,
    fallbackType = FALLBACK_VIDEO_TYPE,
    onPicked = onPicked,
)

@Composable
private fun rememberPicker(
    request: ActivityResultContracts.PickVisualMedia.VisualMediaType,
    fallbackName: String,
    fallbackType: String,
    onPicked: (PickedMedia) -> Unit,
): MediaPicker {
    val context = LocalContext.current
    val logger: Logger = koinInject()
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val picked = withContext(Dispatchers.IO) {
                context.readMedia(
                    uri = uri,
                    fallbackName = fallbackName,
                    fallbackType = fallbackType,
                    logger = logger,
                )
            }
            if (picked != null) onPicked(picked)
        }
    }
    return remember(launcher, request) {
        MediaPicker { launcher.launch(PickVisualMediaRequest(request)) }
    }
}

private fun Context.readMedia(
    uri: Uri,
    fallbackName: String,
    fallbackType: String,
    logger: Logger,
): PickedMedia? {
    return runCatching {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Не удалось открыть поток для $uri")
        PickedMedia(
            fileName = displayNameOf(uri) ?: fallbackName,
            contentType = contentResolver.getType(uri) ?: fallbackType,
            bytes = bytes,
        )
    }.getOrElse { failure ->
        logger.error(tag = LOG_TAG, message = "Не удалось прочитать выбранный файл", throwable = failure)
        null
    }
}

private fun Context.displayNameOf(uri: Uri): String? {
    val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor?.use { row ->
        if (!row.moveToFirst()) return@use null
        val columnIndex = row.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex < 0) null else row.getString(columnIndex)
    }
}
