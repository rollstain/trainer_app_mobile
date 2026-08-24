package app.trainer.uikit.semantic

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class AppTypography(
    val display: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val numeric: TextStyle,
    val numericBig: TextStyle,
    val overline: TextStyle,
    val inviteCode: TextStyle,
)

fun appTypography(
    textFontFamily: FontFamily = FontFamily.Default,
    numericFontFamily: FontFamily = FontFamily.Monospace,
): AppTypography = AppTypography(
    display = TextStyle(
        fontFamily = textFontFamily,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    title = TextStyle(
        fontFamily = textFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    headline = TextStyle(
        fontFamily = textFontFamily,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    body = TextStyle(
        fontFamily = textFontFamily,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyStrong = TextStyle(
        fontFamily = textFontFamily,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    label = TextStyle(
        fontFamily = textFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    caption = TextStyle(
        fontFamily = textFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
    ),
    numeric = TextStyle(
        fontFamily = numericFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    numericBig = TextStyle(
        fontFamily = numericFontFamily,
        fontSize = 24.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    overline = TextStyle(
        fontFamily = numericFontFamily,
        fontSize = 11.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    ),
    inviteCode = TextStyle(
        fontFamily = numericFontFamily,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.em,
    ),
)
