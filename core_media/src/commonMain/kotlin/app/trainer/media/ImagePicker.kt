package app.trainer.media

import androidx.compose.runtime.Composable

class PickedImage(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
)

fun interface ImagePicker {

    fun pick()
}

@Composable
expect fun rememberImagePicker(onPicked: (PickedImage) -> Unit): ImagePicker
