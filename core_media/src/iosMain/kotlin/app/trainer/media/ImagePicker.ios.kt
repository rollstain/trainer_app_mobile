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

private const val LOG_TAG = "image-picker"
private const val JPEG_TYPE_IDENTIFIER = "public.jpeg"
private const val JPEG_CONTENT_TYPE = "image/jpeg"
private const val FALLBACK_FILE_NAME = "photo.jpg"
private const val SELECTION_LIMIT = 1L

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): ImagePicker {
    val logger: Logger = koinInject()
    val delegate = remember(onPicked) { ImagePickerDelegate(onPicked = onPicked, logger = logger) }
    return remember(delegate) {
        ImagePicker {
            val configuration = PHPickerConfiguration().apply {
                setFilter(PHPickerFilter.imagesFilter)
                setSelectionLimit(SELECTION_LIMIT)
            }
            val controller = PHPickerViewController(configuration = configuration)
            controller.delegate = delegate
            val presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (presenter == null) {
                logger.error(tag = LOG_TAG, message = "Не найден rootViewController для показа выбора фото")
            } else {
                presenter.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}

private class ImagePickerDelegate(
    private val onPicked: (PickedImage) -> Unit,
    private val logger: Logger,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(flag = true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        val provider = result.itemProvider
        val fileName = provider.suggestedName?.let { "$it.jpg" } ?: FALLBACK_FILE_NAME
        provider.loadDataRepresentationForTypeIdentifier(JPEG_TYPE_IDENTIFIER) { data, error ->
            deliver(data = data, error = error, fileName = fileName)
        }
    }

    private fun deliver(data: NSData?, error: NSError?, fileName: String) {
        if (data == null) {
            logger.error(
                tag = LOG_TAG,
                message = "Не удалось прочитать выбранное изображение: ${error?.localizedDescription}",
            )
            return
        }
        val picked = PickedImage(
            fileName = fileName,
            contentType = JPEG_CONTENT_TYPE,
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
