package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

const val CODE_LENGTH = 6

private val CELL_HEIGHT = 56.dp
private val CELL_GAP = 8.dp
private const val DISABLED_CELL_ALPHA = 0.6f

enum class CodeInputState { Typing, Checking, Error }

@Composable
fun AppCodeInput(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    state: CodeInputState = CodeInputState.Typing,
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isEditable = state != CodeInputState.Checking

    LaunchedEffect(isEditable) {
        if (isEditable) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        CodeCells(
            value = value,
            state = state,
            activeIndex = if (isFocused) value.length else null,
        )
        BasicTextField(
            value = value,
            onValueChange = { typed -> onValueChange(normalized(typed)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(CELL_HEIGHT)
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource),
            enabled = isEditable,
            textStyle = AppTheme.typography.inviteCode.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            interactionSource = interactionSource,
            decorationBox = { Box(modifier = Modifier.fillMaxWidth().height(CELL_HEIGHT)) },
        )
    }
}

@Composable
private fun CodeCells(value: String, state: CodeInputState, activeIndex: Int?) {
    val colors = AppTheme.colors
    val alpha = if (state == CodeInputState.Checking) DISABLED_CELL_ALPHA else 1f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        repeat(CODE_LENGTH) { index ->
            val symbol = value.getOrNull(index)
            val isActive = activeIndex == index && state != CodeInputState.Checking
            val borderColor = when {
                state == CodeInputState.Error -> colors.danger
                isActive -> colors.accent
                symbol != null -> colors.borderStrong
                else -> colors.border
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(CELL_HEIGHT)
                    .aspectRatio(ratio = 1f, matchHeightConstraintsFirst = true)
                    .background(
                        color = if (state == CodeInputState.Error) colors.dangerSoft else colors.bgSurface,
                        shape = RoundedCornerShape(AppTheme.radius.dp8),
                    )
                    .border(
                        width = if (isActive) AppTheme.borders.focus else AppTheme.borders.hairline,
                        color = borderColor.copy(alpha = borderColor.alpha * alpha),
                        shape = RoundedCornerShape(AppTheme.radius.dp8),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = symbol?.toString().orEmpty(),
                    style = AppTheme.typography.inviteCode,
                    color = if (state == CodeInputState.Error) colors.danger else colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun normalized(typed: String): String =
    typed.filter(Char::isLetterOrDigit).uppercase().take(CODE_LENGTH)
