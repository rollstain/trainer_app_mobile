package app.trainer.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import app.trainer.base.date.filterTimeDigits
import app.trainer.uikit.AppTheme
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.TextFieldKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CARET_AFTER_HOUR = 2

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = android.app.Application::class,
    qualifiers = "w411dp-h891dp-xhdpi",
)
class TextFieldCursorTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setTimeField(initial: String) {
        compose.setContent {
            AppTheme(textFontFamily = FontFamily.Default, numericFontFamily = FontFamily.Monospace) {
                var digits by remember { mutableStateOf(initial) }
                AppTextField(
                    value = digits,
                    onValueChange = { digits = filterTimeDigits(it) },
                    kind = TextFieldKind.Numeric,
                )
            }
        }
    }

    @Test
    fun `a symbol the model rejects leaves the caret where it was typed`() {
        setTimeField(initial = "1234")

        compose.onNodeWithText("1234").performTextInputSelection(TextRange(CARET_AFTER_HOUR))
        compose.onNodeWithText("1234").performTextInput("a")
        compose.waitForIdle()

        compose.onNodeWithText("1234").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.TextSelectionRange,
                TextRange(CARET_AFTER_HOUR),
            ),
        )
    }

    @Test
    fun `a digit the model expands moves the caret past what it added`() {
        setTimeField(initial = "")

        compose.onNodeWithText("").performTextInput("9")
        compose.waitForIdle()

        compose.onNodeWithText("09").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.TextSelectionRange,
                TextRange(CARET_AFTER_HOUR),
            ),
        )
    }
}
