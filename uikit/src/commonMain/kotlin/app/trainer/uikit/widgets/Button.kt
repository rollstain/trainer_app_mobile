package app.trainer.uikit.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private const val LOADING_ALPHA = 0.8f
private val SPINNER_SIZE = 14.dp
private val SPINNER_STROKE = 2.dp
private val CONTENT_GAP = 8.dp
private val LOADING_GAP = 10.dp

enum class ButtonTone { Primary, Secondary, Text, Danger }

enum class ButtonSize { Small, Medium, Large }

enum class ButtonState { Idle, Loading, Disabled }

sealed interface ButtonIcon {

    data object None : ButtonIcon

    class Content(val render: @Composable () -> Unit) : ButtonIcon
}

@Composable
fun AppButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    tone: ButtonTone = ButtonTone.Primary,
    size: ButtonSize = ButtonSize.Medium,
    state: ButtonState = ButtonState.Idle,
    icon: ButtonIcon = ButtonIcon.None,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val palette = buttonPalette(tone = tone, state = state, isPressed = isPressed)
    val isClickable = state == ButtonState.Idle

    Row(
        modifier = modifier
            .heightIn(min = heightOf(size))
            .defaultMinSize(minWidth = AppTheme.sizing.minTouchTarget)
            .background(color = palette.background, shape = RoundedCornerShape(AppTheme.radius.dp8))
            .then(borderModifier(palette.border))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isClickable,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPaddingOf(size))
            .alpha(if (state == ButtonState.Loading) LOADING_ALPHA else 1f),
        horizontalArrangement = Arrangement.spacedBy(
            space = if (state == ButtonState.Loading) LOADING_GAP else CONTENT_GAP,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            ButtonState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(SPINNER_SIZE),
                color = palette.content,
                strokeWidth = SPINNER_STROKE,
            )
            ButtonState.Idle, ButtonState.Disabled -> when (icon) {
                ButtonIcon.None -> Unit
                is ButtonIcon.Content -> icon.render()
            }
        }
        Text(
            text = text,
            style = AppTheme.typography.label,
            color = palette.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class ButtonPalette(
    val background: Color,
    val content: Color,
    val border: BorderStroke?,
)

@Composable
private fun buttonPalette(tone: ButtonTone, state: ButtonState, isPressed: Boolean): ButtonPalette {
    val colors = AppTheme.colors
    if (state == ButtonState.Disabled) {
        return ButtonPalette(
            background = colors.bgSurfaceSunken,
            content = colors.textMuted,
            border = null,
        )
    }
    return when (tone) {
        ButtonTone.Primary -> ButtonPalette(
            background = if (isPressed) colors.accentPressed else colors.accent,
            content = colors.accentOn,
            border = null,
        )
        ButtonTone.Secondary -> ButtonPalette(
            background = if (isPressed) colors.bgSurfaceSunken else colors.bgSurface,
            content = colors.textPrimary,
            border = BorderStroke(width = AppTheme.borders.hairline, color = colors.borderStrong),
        )
        ButtonTone.Text -> ButtonPalette(
            background = if (isPressed) colors.accentSoft else Color.Transparent,
            content = colors.accent,
            border = null,
        )
        ButtonTone.Danger -> ButtonPalette(
            background = colors.danger,
            content = colors.accentOn,
            border = null,
        )
    }
}

@Composable
private fun borderModifier(border: BorderStroke?): Modifier {
    if (border == null) return Modifier
    return Modifier.border(border = border, shape = RoundedCornerShape(AppTheme.radius.dp8))
}

@Composable
private fun heightOf(size: ButtonSize): Dp = when (size) {
    ButtonSize.Small -> AppTheme.sizing.buttonSmall
    ButtonSize.Medium -> AppTheme.sizing.buttonMedium
    ButtonSize.Large -> AppTheme.sizing.buttonLarge
}

@Composable
private fun horizontalPaddingOf(size: ButtonSize): Dp = when (size) {
    ButtonSize.Small -> AppTheme.sizing.buttonPaddingSmall
    ButtonSize.Medium -> AppTheme.sizing.buttonPaddingMedium
    ButtonSize.Large -> AppTheme.sizing.buttonPaddingLarge
}
