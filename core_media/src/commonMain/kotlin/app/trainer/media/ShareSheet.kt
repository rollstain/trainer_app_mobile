package app.trainer.media

import androidx.compose.runtime.Composable

fun interface ShareSheet {

    fun share(text: String)
}

@Composable
expect fun rememberShareSheet(): ShareSheet
