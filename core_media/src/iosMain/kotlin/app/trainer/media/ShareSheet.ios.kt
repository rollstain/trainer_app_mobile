package app.trainer.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.trainer.logger.Logger
import org.koin.compose.koinInject
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val LOG_TAG = "share-sheet"

@Composable
actual fun rememberShareSheet(): ShareSheet {
    val logger: Logger = koinInject()
    return remember(logger) {
        ShareSheet { text ->
            dispatch_async(dispatch_get_main_queue()) {
                val presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
                if (presenter == null) {
                    logger.error(tag = LOG_TAG, message = "Не найден rootViewController для показа «Поделиться»")
                    return@dispatch_async
                }
                val controller = UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null,
                )
                controller.popoverPresentationController?.sourceView = presenter.view
                presenter.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}
