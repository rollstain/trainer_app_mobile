package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val FIELD_PADDING = 14.dp
private val MULTILINE_PADDING_VERTICAL = 12.dp
private val LABEL_GAP = 6.dp
private val UNIT_GAP = 6.dp

enum class TextFieldKind { Text, Multiline, Numeric, InviteCode }

sealed interface TextFieldLabel {

    data object None : TextFieldLabel

    data class Text(val value: String) : TextFieldLabel
}

sealed interface TextFieldMessage {

    data object None : TextFieldMessage

    data class Error(val value: String) : TextFieldMessage
}

sealed interface TextFieldUnit {

    data object None : TextFieldUnit

    data class Text(val value: String) : TextFieldUnit
}

@Composable
fun AppTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    kind: TextFieldKind = TextFieldKind.Text,
    label: TextFieldLabel = TextFieldLabel.None,
    message: TextFieldMessage = TextFieldMessage.None,
    placeholder: String = "",
    isEnabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val hasError = message is TextFieldMessage.Error
    val colors = AppTheme.colors

    val borderColor = when {
        !isEnabled -> colors.border
        hasError -> colors.danger
        isFocused -> colors.accent
        else -> colors.borderStrong
    }
    val borderWidth = when {
        hasError -> AppTheme.borders.field
        isFocused -> AppTheme.borders.focus
        else -> AppTheme.borders.hairline
    }
    val background = when {
        hasError -> colors.dangerSoft
        !isEnabled -> colors.bgSurfaceSunken
        else -> colors.bgSurface
    }
    val labelColor = when {
        hasError -> colors.danger
        isFocused -> colors.accent
        else -> colors.textSecondary
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LABEL_GAP)) {
        when (label) {
            TextFieldLabel.None -> Unit
            is TextFieldLabel.Text -> Text(
                text = label.value,
                style = AppTheme.typography.label,
                color = labelColor,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeightOf(kind))
                .background(color = background, shape = RoundedCornerShape(AppTheme.radius.dp8))
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(AppTheme.radius.dp8),
                )
                .padding(
                    horizontal = FIELD_PADDING,
                    vertical = if (kind == TextFieldKind.Multiline) MULTILINE_PADDING_VERTICAL else 0.dp,
                ),
            verticalAlignment = if (kind == TextFieldKind.Multiline) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UNIT_GAP),
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = alignmentOf(kind)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyleOf(kind),
                        color = colors.textMuted,
                        textAlign = textAlignOf(kind),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled,
                    textStyle = textStyleOf(kind).copy(
                        color = if (isEnabled) colors.textPrimary else colors.textMuted,
                        textAlign = textAlignOf(kind),
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardTypeOf(kind)),
                    singleLine = kind != TextFieldKind.Multiline,
                    cursorBrush = SolidColor(colors.accent),
                    interactionSource = interactionSource,
                )
            }
        }
        when (message) {
            TextFieldMessage.None -> Unit
            is TextFieldMessage.Error -> Text(
                text = message.value,
                style = AppTheme.typography.caption,
                color = colors.danger,
            )
        }
    }
}

@Composable
fun AppNumericField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    unit: TextFieldUnit = TextFieldUnit.None,
    placeholder: String = "",
    isEnabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UNIT_GAP),
    ) {
        AppTextField(
            modifier = Modifier
                .weight(1f)
                .height(AppTheme.sizing.setFieldHeight),
            value = value,
            onValueChange = onValueChange,
            kind = TextFieldKind.Numeric,
            placeholder = placeholder,
            isEnabled = isEnabled,
        )
        when (unit) {
            TextFieldUnit.None -> Unit
            is TextFieldUnit.Text -> Text(
                text = unit.value,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun minHeightOf(kind: TextFieldKind) = when (kind) {
    TextFieldKind.Text, TextFieldKind.Numeric -> AppTheme.sizing.fieldHeight
    TextFieldKind.Multiline -> AppTheme.sizing.fieldMultilineMinHeight
    TextFieldKind.InviteCode -> AppTheme.sizing.inviteCodeFieldHeight
}

@Composable
private fun textStyleOf(kind: TextFieldKind): TextStyle = when (kind) {
    TextFieldKind.Text, TextFieldKind.Multiline -> AppTheme.typography.body
    TextFieldKind.Numeric -> AppTheme.typography.numeric
    TextFieldKind.InviteCode -> AppTheme.typography.inviteCode
}

private fun textAlignOf(kind: TextFieldKind): TextAlign = when (kind) {
    TextFieldKind.Text, TextFieldKind.Multiline -> TextAlign.Start
    TextFieldKind.Numeric -> TextAlign.End
    TextFieldKind.InviteCode -> TextAlign.Center
}

private fun alignmentOf(kind: TextFieldKind): Alignment = when (kind) {
    TextFieldKind.Text, TextFieldKind.Multiline -> Alignment.CenterStart
    TextFieldKind.Numeric -> Alignment.CenterEnd
    TextFieldKind.InviteCode -> Alignment.Center
}

private fun keyboardTypeOf(kind: TextFieldKind): KeyboardType = when (kind) {
    TextFieldKind.Text, TextFieldKind.Multiline -> KeyboardType.Text
    TextFieldKind.Numeric -> KeyboardType.Decimal
    TextFieldKind.InviteCode -> KeyboardType.Ascii
}
