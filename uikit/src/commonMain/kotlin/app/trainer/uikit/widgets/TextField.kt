package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.resources.Res
import app.trainer.uikit.resources.field_clear
import app.trainer.uikit.resources.field_hide
import app.trainer.uikit.resources.field_show
import app.trainer.uikit.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val FIELD_PADDING = 14.dp
private val ACTION_PADDING_END = 8.dp
private val ACTION_FIELD_HEIGHT = 56.dp
private val ACTION_HEIGHT = 40.dp
private val ACTION_TEXT_PADDING = 8.dp
private val ACTION_TEXT_PADDING_VERTICAL = 10.dp
private val MULTILINE_PADDING_VERTICAL = 12.dp
private val LABEL_GAP = 6.dp
private val UNIT_GAP = 6.dp

enum class TextFieldKind { Text, Multiline, Numeric, InviteCode, Email, Password, NewPassword }

sealed interface TextFieldLabel {

    data object None : TextFieldLabel

    data class Text(val value: String) : TextFieldLabel
}

sealed interface TextFieldMessage {

    data object None : TextFieldMessage

    data class Neutral(val value: String) : TextFieldMessage

    data class Warning(val value: String) : TextFieldMessage

    data class Success(val value: String) : TextFieldMessage

    data class Error(val value: String) : TextFieldMessage
}

sealed interface TextFieldAction {

    data object None : TextFieldAction

    data class Reveal(val isRevealed: Boolean, val onToggle: () -> Unit) : TextFieldAction

    data class Clear(val onClear: () -> Unit) : TextFieldAction
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
    action: TextFieldAction = TextFieldAction.None,
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
                    start = FIELD_PADDING,
                    end = if (action == TextFieldAction.None) FIELD_PADDING else ACTION_PADDING_END,
                    top = if (kind == TextFieldKind.Multiline) MULTILINE_PADDING_VERTICAL else 0.dp,
                    bottom = if (kind == TextFieldKind.Multiline) MULTILINE_PADDING_VERTICAL else 0.dp,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofillOf(kind),
                    enabled = isEnabled,
                    textStyle = textStyleOf(kind).copy(
                        color = if (isEnabled) colors.textPrimary else colors.textMuted,
                        textAlign = textAlignOf(kind),
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = keyboardTypeOf(kind),
                    ),
                    visualTransformation = if (isMasked(kind = kind, action = action)) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    singleLine = kind != TextFieldKind.Multiline,
                    cursorBrush = SolidColor(colors.accent),
                    interactionSource = interactionSource,
                )
            }
            FieldAction(action = action, isEnabled = isEnabled)
        }
        val messageText = textOf(message)
        if (messageText != null) {
            Text(
                text = messageText,
                style = AppTheme.typography.caption,
                color = toneOf(message),
            )
        }
    }
}

@Composable
private fun FieldAction(action: TextFieldAction, isEnabled: Boolean) {
    when (action) {
        TextFieldAction.None -> Unit
        is TextFieldAction.Reveal -> Text(
            modifier = Modifier
                .heightIn(min = ACTION_HEIGHT)
                .clickable(enabled = isEnabled, onClick = action.onToggle)
                .padding(horizontal = ACTION_TEXT_PADDING, vertical = ACTION_TEXT_PADDING_VERTICAL),
            text = if (action.isRevealed) {
                stringResource(Res.string.field_hide)
            } else {
                stringResource(Res.string.field_show)
            },
            style = AppTheme.typography.label,
            color = AppTheme.colors.accent,
        )
        is TextFieldAction.Clear -> AppIcon(
            modifier = Modifier
                .heightIn(min = ACTION_HEIGHT)
                .clickable(enabled = isEnabled, onClick = action.onClear)
                .padding(horizontal = ACTION_TEXT_PADDING),
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = stringResource(Res.string.field_clear),
            size = IconSize.Medium,
            tint = AppTheme.colors.textMuted,
        )
    }
}

private fun isMasked(kind: TextFieldKind, action: TextFieldAction): Boolean {
    val hides = kind == TextFieldKind.Password || kind == TextFieldKind.NewPassword
    if (!hides) return false
    return (action as? TextFieldAction.Reveal)?.isRevealed != true
}

private fun Modifier.autofillOf(kind: TextFieldKind): Modifier {
    val type = when (kind) {
        TextFieldKind.Email -> ContentType.Username + ContentType.EmailAddress
        TextFieldKind.Password -> ContentType.Password
        TextFieldKind.NewPassword -> ContentType.NewPassword
        TextFieldKind.Text, TextFieldKind.Multiline, TextFieldKind.Numeric, TextFieldKind.InviteCode -> null
    } ?: return this
    return semantics { contentType = type }
}

private fun textOf(message: TextFieldMessage): String? {
    val value = when (message) {
        TextFieldMessage.None -> null
        is TextFieldMessage.Neutral -> message.value
        is TextFieldMessage.Warning -> message.value
        is TextFieldMessage.Success -> message.value
        is TextFieldMessage.Error -> message.value
    }
    return value?.takeIf { it.isNotEmpty() }
}

@Composable
private fun toneOf(message: TextFieldMessage): Color = when (message) {
    TextFieldMessage.None, is TextFieldMessage.Neutral -> AppTheme.colors.textSecondary
    is TextFieldMessage.Warning -> AppTheme.colors.warning
    is TextFieldMessage.Success -> AppTheme.colors.success
    is TextFieldMessage.Error -> AppTheme.colors.danger
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
                .heightIn(min = AppTheme.sizing.setFieldHeight),
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
    TextFieldKind.Email, TextFieldKind.Password, TextFieldKind.NewPassword -> ACTION_FIELD_HEIGHT
    TextFieldKind.Multiline -> AppTheme.sizing.fieldMultilineMinHeight
    TextFieldKind.InviteCode -> AppTheme.sizing.inviteCodeFieldHeight
}

@Composable
private fun textStyleOf(kind: TextFieldKind): TextStyle = when (kind) {
    TextFieldKind.Text,
    TextFieldKind.Multiline,
    TextFieldKind.Email,
    TextFieldKind.Password,
    TextFieldKind.NewPassword,
    -> AppTheme.typography.body
    TextFieldKind.Numeric -> AppTheme.typography.numeric
    TextFieldKind.InviteCode -> AppTheme.typography.inviteCode
}

private fun textAlignOf(kind: TextFieldKind): TextAlign = when (kind) {
    TextFieldKind.Text,
    TextFieldKind.Multiline,
    TextFieldKind.Email,
    TextFieldKind.Password,
    TextFieldKind.NewPassword,
    -> TextAlign.Start
    TextFieldKind.Numeric -> TextAlign.End
    TextFieldKind.InviteCode -> TextAlign.Center
}

private fun alignmentOf(kind: TextFieldKind): Alignment = when (kind) {
    TextFieldKind.Text,
    TextFieldKind.Multiline,
    TextFieldKind.Email,
    TextFieldKind.Password,
    TextFieldKind.NewPassword,
    -> Alignment.CenterStart
    TextFieldKind.Numeric -> Alignment.CenterEnd
    TextFieldKind.InviteCode -> Alignment.Center
}

private fun keyboardTypeOf(kind: TextFieldKind): KeyboardType = when (kind) {
    TextFieldKind.Text, TextFieldKind.Multiline -> KeyboardType.Text
    TextFieldKind.Email -> KeyboardType.Email
    TextFieldKind.Password, TextFieldKind.NewPassword -> KeyboardType.Password
    TextFieldKind.Numeric -> KeyboardType.Decimal
    TextFieldKind.InviteCode -> KeyboardType.Ascii
}
