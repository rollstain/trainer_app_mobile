package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trainer.uikit.AppTheme

private const val MAX_INITIALS = 2
private const val STACK_MAX_AVATARS = 4
private const val HIDDEN_PREFIX = "+"
private val STACK_OVERLAP = 7.dp
private val STACK_RING_WIDTH = 2.dp

enum class AvatarSize { Stack, Small, Medium, Large, Hero }

enum class AvatarTone { Active, Neutral }

@Composable
fun AppAvatar(
    modifier: Modifier = Modifier,
    displayName: String,
    size: AvatarSize = AvatarSize.Large,
    tone: AvatarTone = AvatarTone.Neutral,
) {
    val background = when (tone) {
        AvatarTone.Active -> AppTheme.colors.accentSoft
        AvatarTone.Neutral -> AppTheme.colors.bgSurfaceSunken
    }
    val content = when (tone) {
        AvatarTone.Active -> AppTheme.colors.accent
        AvatarTone.Neutral -> AppTheme.colors.textSecondary
    }
    Box(
        modifier = modifier
            .size(diameterOf(size))
            .background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(displayName),
            style = initialsStyleOf(size),
            color = content,
        )
    }
}

private fun initialsOf(displayName: String): String {
    return displayName
        .split(' ')
        .filter { it.isNotBlank() }
        .take(MAX_INITIALS)
        .map { it.first().uppercaseChar() }
        .joinToString(separator = "")
}

@Composable
private fun initialsStyleOf(size: AvatarSize): TextStyle = when (size) {
    AvatarSize.Stack -> AppTheme.typography.caption
    AvatarSize.Hero -> AppTheme.typography.headline
    AvatarSize.Small, AvatarSize.Medium, AvatarSize.Large -> AppTheme.typography.label
}

@Composable
private fun diameterOf(size: AvatarSize): Dp = when (size) {
    AvatarSize.Stack -> AppTheme.sizing.avatarStack
    AvatarSize.Small -> AppTheme.sizing.avatarSmall
    AvatarSize.Medium -> AppTheme.sizing.avatarMedium
    AvatarSize.Large -> AppTheme.sizing.avatarLarge
    AvatarSize.Hero -> AppTheme.sizing.avatarHero
}

@Composable
fun AppAvatarStack(modifier: Modifier = Modifier, names: List<String>) {
    if (names.isEmpty()) return
    val shown = names.take(STACK_MAX_AVATARS)
    val hidden = names.size - shown.size
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-STACK_OVERLAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEach { name ->
            AppAvatar(
                modifier = Modifier.stackRing(),
                displayName = name,
                size = AvatarSize.Stack,
            )
        }
        if (hidden > 0) {
            Box(
                modifier = Modifier
                    .stackRing()
                    .size(AppTheme.sizing.avatarStack)
                    .background(color = AppTheme.colors.bgSurfaceSunken, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = HIDDEN_PREFIX + hidden,
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun Modifier.stackRing(): Modifier = border(
    width = STACK_RING_WIDTH,
    color = AppTheme.colors.bgSurface,
    shape = CircleShape,
)
