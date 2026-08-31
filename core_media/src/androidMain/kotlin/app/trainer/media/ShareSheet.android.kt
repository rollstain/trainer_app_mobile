package app.trainer.media

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val SHARE_TEXT_TYPE = "text/plain"

@Composable
actual fun rememberShareSheet(): ShareSheet {
    val context = LocalContext.current
    return remember(context) {
        ShareSheet { text ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = SHARE_TEXT_TYPE
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
    }
}
