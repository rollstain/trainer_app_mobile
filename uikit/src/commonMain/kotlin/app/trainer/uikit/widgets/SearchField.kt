package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val SEARCH_FIELD_HEIGHT = 44.dp
private val FIELD_PADDING = 12.dp

@Composable
fun AppSearchField(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    clearDescription: String,
) {
    val colors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(AppTheme.radius.dp8)

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = SEARCH_FIELD_HEIGHT)
            .background(color = if (isFocused) colors.bgSurfaceSunken else colors.bgScreen, shape = shape)
            .then(
                if (isFocused) {
                    Modifier.border(width = AppTheme.borders.focus, color = colors.accent, shape = shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = FIELD_PADDING),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            painter = AppIcons.search,
            contentDescription = null,
            size = IconSize.Small,
            tint = colors.textMuted,
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                AppText(
                    text = placeholder,
                    style = AppTheme.typography.body,
                    color = colors.textMuted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(colors.accent),
                textStyle = AppTheme.typography.body.copy(color = colors.textPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        if (value.isNotEmpty()) {
            AppIconButton(
                painter = AppIcons.close,
                contentDescription = clearDescription,
                onClick = onClear,
                size = IconSize.Small,
            )
        }
    }
}
