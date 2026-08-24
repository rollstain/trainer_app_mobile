package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private val TOP_BAR_ACTION_SIZE = 40.dp
private val ACTIVE_TAB_INDICATOR_HEIGHT = 2.dp

sealed interface TopBarLeading {

    data object None : TopBarLeading

    data class Back(val onClick: () -> Unit) : TopBarLeading
}

sealed interface TopBarSubtitle {

    data object None : TopBarSubtitle

    data class Text(val value: String) : TopBarSubtitle
}

sealed interface TopBarAction {

    data object None : TopBarAction

    class Content(val onClick: () -> Unit, val render: @Composable () -> Unit) : TopBarAction
}

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    title: String,
    leading: TopBarLeading = TopBarLeading.None,
    subtitle: TopBarSubtitle = TopBarSubtitle.None,
    action: TopBarAction = TopBarAction.None,
    avatarName: String = "",
) {
    Column(modifier = modifier.fillMaxWidth().background(AppTheme.colors.bgSurface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.sizing.topBarHeight)
                .padding(horizontal = AppTheme.spacing.dp8),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (leading) {
                TopBarLeading.None -> Box(modifier = Modifier.width(AppTheme.spacing.dp8))
                is TopBarLeading.Back -> AppIconButton(
                    painter = AppIcons.back,
                    contentDescription = "Назад",
                    onClick = leading.onClick,
                )
            }
            if (avatarName.isNotEmpty()) {
                AppAvatar(displayName = avatarName, size = AvatarSize.Medium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = if (subtitle == TopBarSubtitle.None) {
                        AppTheme.typography.title
                    } else {
                        AppTheme.typography.bodyStrong
                    },
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when (subtitle) {
                    TopBarSubtitle.None -> Unit
                    is TopBarSubtitle.Text -> Text(
                        text = subtitle.value,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
            when (action) {
                TopBarAction.None -> Unit
                is TopBarAction.Content -> Box(
                    modifier = Modifier
                        .size(TOP_BAR_ACTION_SIZE)
                        .clickable(onClick = action.onClick),
                    contentAlignment = Alignment.Center,
                    content = { action.render() },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.borders.hairline)
                .background(AppTheme.colors.border),
        )
    }
}

class BottomNavItem(
    val id: String,
    val label: String,
    val icon: @Composable (Boolean) -> Painter,
)

@Composable
fun AppBottomNavigation(
    modifier: Modifier = Modifier,
    items: List<BottomNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().background(AppTheme.colors.bgSurface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.borders.hairline)
                .background(AppTheme.colors.border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.sizing.bottomBarHeight),
        ) {
            items.forEach { item ->
                val isSelected = item.id == selectedId
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(item.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ACTIVE_TAB_INDICATOR_HEIGHT)
                            .background(
                                if (isSelected) AppTheme.colors.accent else AppTheme.colors.bgSurface
                            ),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val tint = if (isSelected) {
                            AppTheme.colors.accent
                        } else {
                            AppTheme.colors.textSecondary
                        }
                        AppIcon(
                            painter = item.icon(isSelected),
                            contentDescription = item.label,
                            tint = tint,
                        )
                        Text(
                            text = item.label,
                            style = AppTheme.typography.caption,
                            color = tint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppToast(modifier: Modifier = Modifier, text: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.sizing.toastHeight)
            .background(
                color = AppTheme.colors.bgInverse,
                shape = RoundedCornerShape(AppTheme.radius.dp8),
            )
            .padding(horizontal = AppTheme.spacing.dp16),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = AppTheme.typography.body,
            color = AppTheme.colors.textInverse,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.clickable(onClick = onAction),
            text = actionText,
            style = AppTheme.typography.label,
            color = AppTheme.colors.accent,
        )
    }
}
