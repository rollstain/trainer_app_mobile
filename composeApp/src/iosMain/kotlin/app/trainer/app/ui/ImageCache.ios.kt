package app.trainer.app.ui

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

private const val IMAGE_CACHE_DIRECTORY = "image_cache"

actual fun imageCacheDirectory(context: PlatformContext): Path {
    val caches = NSSearchPathForDirectoriesInDomains(
        directory = NSCachesDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).firstOrNull() as? String ?: error("Не удалось получить каталог кеша iOS")
    return "$caches/$IMAGE_CACHE_DIRECTORY".toPath()
}
