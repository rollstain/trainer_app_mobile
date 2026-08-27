package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.trainer.uikit.AppTheme

enum class ListCellSize { Small, Large }

sealed interface ListCellPreview {

    data object None : ListCellPreview

    data class Text(val value: String) : ListCellPreview
}

sealed interface ListCellTrailing {

    data object None : ListCellTrailing

    data class TimeWithBadge(val time: String, val unreadCount: Long) : ListCellTrailing

    data class Time(val value: String) : ListCellTrailing

    data class Unread(val unreadCount: Long) : ListCellTrailing

    data class Alert(val value: String, val unit: String) : ListCellTrailing

    data class Warning(val text: String) : ListCellTrailing
}

@Composable
fun AppListCell(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    size: ListCellSize = ListCellSize.Large,
    avatarName: String = title,
    preview: ListCellPreview = ListCellPreview.None,
    trailing: ListCellTrailing = ListCellTrailing.None,
) {
    val hasUnread = when (trailing) {
        is ListCellTrailing.TimeWithBadge -> trailing.unreadCount > 0
        is ListCellTrailing.Unread -> trailing.unreadCount > 0
        ListCellTrailing.None,
        is ListCellTrailing.Time,
        is ListCellTrailing.Alert,
        is ListCellTrailing.Warning,
        -> false
    }
    Column(modifier = modifier.background(AppTheme.colors.bgSurface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = heightOf(size))
                .clickable(onClick = onClick)
                .padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp12),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppAvatar(
                displayName = avatarName,
                size = avatarOf(size),
                tone = if (hasUnread) AvatarTone.Active else AvatarTone.Neutral,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
            ) {
                Text(
                    text = title,
                    style = AppTheme.typography.body.copy(
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when (preview) {
                    ListCellPreview.None -> Unit
                    is ListCellPreview.Text -> Text(
                        text = preview.value,
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TrailingContent(trailing = trailing)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.borders.hairline)
                .background(AppTheme.colors.border),
        )
    }
}

@Composable
private fun TrailingContent(trailing: ListCellTrailing) {
    when (trailing) {
        ListCellTrailing.None -> Unit
        is ListCellTrailing.Time -> Text(
            text = trailing.value,
            style = AppTheme.typography.overline,
            color = AppTheme.colors.textMuted,
        )
        is ListCellTrailing.Unread -> AppBadge(value = BadgeValue.Count(trailing.unreadCount))
        is ListCellTrailing.Warning -> Text(
            text = trailing.text,
            style = AppTheme.typography.caption,
            color = AppTheme.colors.warning,
        )
        is ListCellTrailing.Alert -> Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = trailing.value,
                style = AppTheme.typography.numericBig,
                color = AppTheme.colors.warning,
            )
            Text(
                text = trailing.unit,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.warning,
            )
        }
        is ListCellTrailing.TimeWithBadge -> Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
        ) {
            Text(
                text = trailing.time,
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
            )
            if (trailing.unreadCount > 0) {
                AppBadge(value = BadgeValue.Count(trailing.unreadCount))
            }
        }
    }
}

private fun avatarOf(size: ListCellSize): AvatarSize = when (size) {
    ListCellSize.Small -> AvatarSize.Small
    ListCellSize.Large -> AvatarSize.Large
}

@Composable
private fun heightOf(size: ListCellSize): Dp = when (size) {
    ListCellSize.Small -> AppTheme.sizing.cellSmall
    ListCellSize.Large -> AppTheme.sizing.cellLarge
}
