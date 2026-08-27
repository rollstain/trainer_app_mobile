package app.trainer.app.ui

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

private const val IMAGE_CACHE_DIRECTORY = "image_cache"

actual fun imageCacheDirectory(context: PlatformContext): Path =
    context.cacheDir.resolve(IMAGE_CACHE_DIRECTORY).toOkioPath()
