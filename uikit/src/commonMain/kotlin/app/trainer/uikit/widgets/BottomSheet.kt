package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val SHEET_PADDING_TOP = 12.dp
private val SHEET_PADDING_HORIZONTAL = 16.dp
private val SHEET_PADDING_BOTTOM = 16.dp

enum class SheetHandle { On, Off }

sealed interface SheetRowIcon {

    data object None : SheetRowIcon

    data class Painted(val painter: Painter) : SheetRowIcon
}

enum class SheetRowTone { Neutral, Danger }

@Composable
fun AppBottomSheetContainer(
    modifier: Modifier = Modifier,
    title: String,
    handle: SheetHandle = SheetHandle.On,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(
                    topStart = AppTheme.radius.dp16,
                    topEnd = AppTheme.radius.dp16,
                    bottomStart = AppTheme.radius.none,
                    bottomEnd = AppTheme.radius.none,
                ),
            )
            .padding(
                top = SHEET_PADDING_TOP,
                start = SHEET_PADDING_HORIZONTAL,
                end = SHEET_PADDING_HORIZONTAL,
                bottom = SHEET_PADDING_BOTTOM,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        if (handle == SheetHandle.On) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = AppTheme.sizing.sheetHandleWidth, height = AppTheme.sizing.sheetHandleHeight)
                    .background(
                        color = AppTheme.colors.borderStrong,
                        shape = RoundedCornerShape(AppTheme.radius.pill),
                    ),
            )
        }
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = AppTheme.typography.headline,
                color = AppTheme.colors.textPrimary,
            )
        }
        content()
    }
}

@Composable
fun AppSheetActionRow(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    icon: SheetRowIcon = SheetRowIcon.None,
    tone: SheetRowTone = SheetRowTone.Neutral,
    hasDivider: Boolean = true,
) {
    val contentColor = when (tone) {
        SheetRowTone.Neutral -> AppTheme.colors.textPrimary
        SheetRowTone.Danger -> AppTheme.colors.danger
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.sizing.sheetRowHeight)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (icon) {
                SheetRowIcon.None -> Unit
                is SheetRowIcon.Painted -> AppIcon(
                    painter = icon.painter,
                    contentDescription = null,
                    size = IconSize.Medium,
                    tint = contentColor,
                )
            }
            Text(
                text = text,
                style = AppTheme.typography.body,
                color = contentColor,
            )
        }
        if (hasDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTheme.borders.hairline)
                    .background(AppTheme.colors.border),
            )
        }
    }
}
