package app.trainer.uikit.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground

private val PROGRESS_SIZE = 32.dp

@Composable
fun AppFullScreenProgress(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().screenBackground(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(PROGRESS_SIZE),
            color = AppTheme.colors.accent,
        )
    }
}
