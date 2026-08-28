package app.trainer.media

import androidx.compose.runtime.Composable

class PickedMedia(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
)

fun interface MediaPicker {

    fun pick()
}

@Composable
expect fun rememberImagePicker(onPicked: (PickedMedia) -> Unit): MediaPicker

@Composable
expect fun rememberVideoPicker(onPicked: (PickedMedia) -> Unit): MediaPicker
