package app.trainer.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

@Composable
actual fun VideoPlayer(modifier: Modifier, url: String) {
    val controller = remember(url) {
        AVPlayerViewController().apply {
            player = NSURL.URLWithString(url)?.let(::AVPlayer)
            showsPlaybackControls = true
        }
    }
    DisposableEffect(controller) {
        onDispose { controller.player?.pause() }
    }
    UIKitView(
        modifier = modifier,
        factory = { controller.view },
    )
}
