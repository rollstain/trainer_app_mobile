package app.trainer.uikit.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import app.trainer.uikit.AppTheme
import app.trainer.uikit.resources.Res
import app.trainer.uikit.resources.ic_add
import app.trainer.uikit.resources.ic_arrow_back
import app.trainer.uikit.resources.ic_arrow_upward
import app.trainer.uikit.resources.ic_attach_file
import app.trainer.uikit.resources.ic_calendar_month
import app.trainer.uikit.resources.ic_calendar_month_filled
import app.trainer.uikit.resources.ic_chat_bubble
import app.trainer.uikit.resources.ic_chat_bubble_filled
import app.trainer.uikit.resources.ic_check
import app.trainer.uikit.resources.ic_done_all
import app.trainer.uikit.resources.ic_chevron_left
import app.trainer.uikit.resources.ic_chevron_right
import app.trainer.uikit.resources.ic_close
import app.trainer.uikit.resources.ic_delete
import app.trainer.uikit.resources.ic_error
import app.trainer.uikit.resources.ic_fitness_center
import app.trainer.uikit.resources.ic_group
import app.trainer.uikit.resources.ic_group_filled
import app.trainer.uikit.resources.ic_list_alt
import app.trainer.uikit.resources.ic_list_alt_filled
import app.trainer.uikit.resources.ic_logout
import app.trainer.uikit.resources.ic_more_vert
import app.trainer.uikit.resources.ic_monitoring
import app.trainer.uikit.resources.ic_monitoring_filled
import app.trainer.uikit.resources.ic_person
import app.trainer.uikit.resources.ic_schedule
import org.jetbrains.compose.resources.painterResource

enum class IconSize { Small, Medium, Large }

object AppIcons {

    val back: Painter @Composable get() = painterResource(Res.drawable.ic_arrow_back)

    val more: Painter @Composable get() = painterResource(Res.drawable.ic_more_vert)

    val previous: Painter @Composable get() = painterResource(Res.drawable.ic_chevron_left)

    val next: Painter @Composable get() = painterResource(Res.drawable.ic_chevron_right)

    val add: Painter @Composable get() = painterResource(Res.drawable.ic_add)

    val close: Painter @Composable get() = painterResource(Res.drawable.ic_close)

    val delete: Painter @Composable get() = painterResource(Res.drawable.ic_delete)

    val attach: Painter @Composable get() = painterResource(Res.drawable.ic_attach_file)

    val send: Painter @Composable get() = painterResource(Res.drawable.ic_arrow_upward)

    val pending: Painter @Composable get() = painterResource(Res.drawable.ic_schedule)

    val failed: Painter @Composable get() = painterResource(Res.drawable.ic_error)

    val sent: Painter @Composable get() = painterResource(Res.drawable.ic_check)

    val read: Painter @Composable get() = painterResource(Res.drawable.ic_done_all)

    val exercise: Painter @Composable get() = painterResource(Res.drawable.ic_fitness_center)

    val logout: Painter @Composable get() = painterResource(Res.drawable.ic_logout)

    val person: Painter @Composable get() = painterResource(Res.drawable.ic_person)

    @Composable
    fun chats(isActive: Boolean): Painter = painterResource(
        if (isActive) Res.drawable.ic_chat_bubble_filled else Res.drawable.ic_chat_bubble
    )

    @Composable
    fun calendar(isActive: Boolean): Painter = painterResource(
        if (isActive) Res.drawable.ic_calendar_month_filled else Res.drawable.ic_calendar_month
    )

    @Composable
    fun logs(isActive: Boolean): Painter = painterResource(
        if (isActive) Res.drawable.ic_list_alt_filled else Res.drawable.ic_list_alt
    )

    @Composable
    fun people(isActive: Boolean): Painter = painterResource(
        if (isActive) Res.drawable.ic_group_filled else Res.drawable.ic_group
    )

    @Composable
    fun progress(isActive: Boolean): Painter = painterResource(
        if (isActive) Res.drawable.ic_monitoring_filled else Res.drawable.ic_monitoring
    )
}

@Composable
fun AppIcon(
    modifier: Modifier = Modifier,
    painter: Painter,
    contentDescription: String?,
    size: IconSize = IconSize.Large,
    tint: Color = AppTheme.colors.textPrimary,
) {
    Icon(
        modifier = modifier.size(sizeOf(size)),
        painter = painter,
        contentDescription = contentDescription,
        tint = tint,
    )
}

@Composable
fun AppIconButton(
    modifier: Modifier = Modifier,
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    size: IconSize = IconSize.Large,
    tint: Color = AppTheme.colors.textPrimary,
) {
    Box(
        modifier = modifier
            .size(AppTheme.sizing.minTouchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            painter = painter,
            contentDescription = contentDescription,
            size = size,
            tint = tint,
        )
    }
}

@Composable
private fun sizeOf(size: IconSize): Dp = when (size) {
    IconSize.Small -> AppTheme.sizing.iconSmall
    IconSize.Medium -> AppTheme.sizing.iconMedium
    IconSize.Large -> AppTheme.sizing.iconLarge
}
