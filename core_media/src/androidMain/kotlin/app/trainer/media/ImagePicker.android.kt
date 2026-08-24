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

private const val LOG_TAG = "image-picker"
private const val FALLBACK_FILE_NAME = "photo.jpg"
private const val FALLBACK_CONTENT_TYPE = "image/jpeg"

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): ImagePicker {
    val context = LocalContext.current
    val logger: Logger = koinInject()
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val picked = withContext(Dispatchers.IO) { context.readImage(uri = uri, logger = logger) }
            if (picked != null) onPicked(picked)
        }
    }
    return remember(launcher) {
        ImagePicker {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}

private fun Context.readImage(uri: Uri, logger: Logger): PickedImage? {
    return runCatching {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Не удалось открыть поток для $uri")
        PickedImage(
            fileName = displayNameOf(uri) ?: FALLBACK_FILE_NAME,
            contentType = contentResolver.getType(uri) ?: FALLBACK_CONTENT_TYPE,
            bytes = bytes,
        )
    }.getOrElse { failure ->
        logger.error(tag = LOG_TAG, message = "Не удалось прочитать выбранное изображение", throwable = failure)
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
