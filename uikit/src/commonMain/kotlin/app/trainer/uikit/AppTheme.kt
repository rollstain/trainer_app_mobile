package app.trainer.uikit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import app.trainer.uikit.semantic.AppBorders
import app.trainer.uikit.semantic.AppColors
import app.trainer.uikit.semantic.AppElevation
import app.trainer.uikit.semantic.AppMotion
import app.trainer.uikit.semantic.AppRadius
import app.trainer.uikit.semantic.AppSizing
import app.trainer.uikit.semantic.AppSpacing
import app.trainer.uikit.semantic.AppTypography
import app.trainer.uikit.semantic.appTypography
import app.trainer.uikit.semantic.darkAppColors
import app.trainer.uikit.semantic.golosTextFamily
import app.trainer.uikit.semantic.jetBrainsMonoFamily
import app.trainer.uikit.semantic.lightAppColors

private val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors не предоставлены: оберните содержимое в AppTheme")
}

private val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTypography не предоставлена: оберните содержимое в AppTheme")
}

private val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }

private val LocalAppRadius = staticCompositionLocalOf { AppRadius() }

private val LocalAppSizing = staticCompositionLocalOf { AppSizing() }

private val LocalAppElevation = staticCompositionLocalOf { AppElevation() }

private val LocalAppBorders = staticCompositionLocalOf { AppBorders() }

private val LocalAppMotion = staticCompositionLocalOf { AppMotion() }

object AppTheme {

    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current

    val radius: AppRadius
        @Composable
        @ReadOnlyComposable
        get() = LocalAppRadius.current

    val sizing: AppSizing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSizing.current

    val elevation: AppElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalAppElevation.current

    val borders: AppBorders
        @Composable
        @ReadOnlyComposable
        get() = LocalAppBorders.current

    val motion: AppMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalAppMotion.current
}

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    textFontFamily: FontFamily = golosTextFamily(),
    numericFontFamily: FontFamily = jetBrainsMonoFamily(),
    content: @Composable () -> Unit,
) {
    val colors = remember(isDarkTheme) { if (isDarkTheme) darkAppColors() else lightAppColors() }
    val typography = remember(textFontFamily, numericFontFamily) {
        appTypography(textFontFamily = textFontFamily, numericFontFamily = numericFontFamily)
    }
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        content = content,
    )
}
