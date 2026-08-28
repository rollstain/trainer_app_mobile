package app.trainer.uikit.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.trainer.uikit.AppTheme

private const val MAX_INITIALS = 2

enum class AvatarSize { Small, Medium, Large, Hero }

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
            style = if (size == AvatarSize.Hero) AppTheme.typography.headline else AppTheme.typography.label,
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
private fun diameterOf(size: AvatarSize): Dp = when (size) {
    AvatarSize.Small -> AppTheme.sizing.avatarSmall
    AvatarSize.Medium -> AppTheme.sizing.avatarMedium
    AvatarSize.Large -> AppTheme.sizing.avatarLarge
    AvatarSize.Hero -> AppTheme.sizing.avatarHero
}
