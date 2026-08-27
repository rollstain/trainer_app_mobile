package app.trainer.app.ui

import coil3.PlatformContext
import okio.Path

expect fun imageCacheDirectory(context: PlatformContext): Path
