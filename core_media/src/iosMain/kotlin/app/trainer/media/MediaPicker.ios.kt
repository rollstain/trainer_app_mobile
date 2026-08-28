package app.trainer.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.trainer.logger.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.koin.compose.koinInject
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

private const val LOG_TAG = "media-picker"
private const val JPEG_TYPE_IDENTIFIER = "public.jpeg"
private const val JPEG_CONTENT_TYPE = "image/jpeg"
private const val MOVIE_TYPE_IDENTIFIER = "public.movie"
private const val MOVIE_CONTENT_TYPE = "video/quicktime"
private const val FALLBACK_IMAGE_NAME = "photo.jpg"
private const val FALLBACK_VIDEO_NAME = "video.mov"
private const val IMAGE_EXTENSION = ".jpg"
private const val VIDEO_EXTENSION = ".mov"
private const val SELECTION_LIMIT = 1L

@Composable
actual fun rememberImagePicker(onPicked: (PickedMedia) -> Unit): MediaPicker = rememberPicker(
    filter = PHPickerFilter.imagesFilter,
    typeIdentifier = JPEG_TYPE_IDENTIFIER,
    contentType = JPEG_CONTENT_TYPE,
    fallbackName = FALLBACK_IMAGE_NAME,
    extension = IMAGE_EXTENSION,
    onPicked = onPicked,
)

@Composable
actual fun rememberVideoPicker(onPicked: (PickedMedia) -> Unit): MediaPicker = rememberPicker(
    filter = PHPickerFilter.videosFilter,
    typeIdentifier = MOVIE_TYPE_IDENTIFIER,
    contentType = MOVIE_CONTENT_TYPE,
    fallbackName = FALLBACK_VIDEO_NAME,
    extension = VIDEO_EXTENSION,
    onPicked = onPicked,
)

@Composable
private fun rememberPicker(
    filter: PHPickerFilter,
    typeIdentifier: String,
    contentType: String,
    fallbackName: String,
    extension: String,
    onPicked: (PickedMedia) -> Unit,
): MediaPicker {
    val logger: Logger = koinInject()
    val delegate = remember(onPicked, typeIdentifier) {
        MediaPickerDelegate(
            typeIdentifier = typeIdentifier,
            contentType = contentType,
            fallbackName = fallbackName,
            extension = extension,
            onPicked = onPicked,
            logger = logger,
        )
    }
    return remember(delegate) {
        MediaPicker {
            val configuration = PHPickerConfiguration().apply {
                setFilter(filter)
                setSelectionLimit(SELECTION_LIMIT)
            }
            val controller = PHPickerViewController(configuration = configuration)
            controller.delegate = delegate
            val presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (presenter == null) {
                logger.error(tag = LOG_TAG, message = "Не найден rootViewController для показа выбора файла")
            } else {
                presenter.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}

private class MediaPickerDelegate(
    private val typeIdentifier: String,
    private val contentType: String,
    private val fallbackName: String,
    private val extension: String,
    private val onPicked: (PickedMedia) -> Unit,
    private val logger: Logger,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(flag = true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        val provider = result.itemProvider
        val fileName = provider.suggestedName?.let { it + extension } ?: fallbackName
        provider.loadDataRepresentationForTypeIdentifier(typeIdentifier) { data, error ->
            deliver(data = data, error = error, fileName = fileName)
        }
    }

    private fun deliver(data: NSData?, error: NSError?, fileName: String) {
        if (data == null) {
            logger.error(
                tag = LOG_TAG,
                message = "Не удалось прочитать выбранный файл: ${error?.localizedDescription}",
            )
            return
        }
        val picked = PickedMedia(
            fileName = fileName,
            contentType = contentType,
            bytes = data.toByteArray(),
        )
        dispatch_async(dispatch_get_main_queue()) { onPicked(picked) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size == 0) return result
    result.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return result
}
