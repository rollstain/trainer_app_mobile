package app.trainer.uikit.semantic

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.trainer.uikit.resources.Res
import app.trainer.uikit.resources.golos_text_medium
import app.trainer.uikit.resources.golos_text_regular
import app.trainer.uikit.resources.golos_text_semibold
import app.trainer.uikit.resources.jetbrains_mono_bold
import app.trainer.uikit.resources.jetbrains_mono_medium
import app.trainer.uikit.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

@Composable
fun golosTextFamily(): FontFamily = FontFamily(
    Font(resource = Res.font.golos_text_regular, weight = FontWeight.Normal),
    Font(resource = Res.font.golos_text_medium, weight = FontWeight.Medium),
    Font(resource = Res.font.golos_text_semibold, weight = FontWeight.SemiBold),
)

@Composable
fun jetBrainsMonoFamily(): FontFamily = FontFamily(
    Font(resource = Res.font.jetbrains_mono_regular, weight = FontWeight.Normal),
    Font(resource = Res.font.jetbrains_mono_medium, weight = FontWeight.Medium),
    Font(resource = Res.font.jetbrains_mono_bold, weight = FontWeight.Bold),
)
