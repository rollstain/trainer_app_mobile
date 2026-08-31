package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
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
    val focusManager = LocalFocusManager.current
    val lastEdit = remember { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
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
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                AppText(
                    text = placeholder,
                    style = AppTheme.typography.body,
                    color = colors.textMuted,
                )
            }
            BasicTextField(
                value = shownValueOf(lastEdit = lastEdit.value, value = value),
                onValueChange = { edited ->
                    lastEdit.value = edited
                    if (edited.text != value) onValueChange(edited.text)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(colors.accent),
                textStyle = AppTheme.typography.body.copy(color = colors.textPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(SEARCH_FIELD_HEIGHT)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    painter = AppIcons.close,
                    contentDescription = clearDescription,
                    size = IconSize.Medium,
                    tint = colors.textMuted,
                )
            }
        }
    }
}
