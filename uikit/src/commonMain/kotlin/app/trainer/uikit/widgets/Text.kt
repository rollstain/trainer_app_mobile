package app.trainer.uikit.widgets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import app.trainer.uikit.AppTheme

@Composable
fun AppText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    color: Color,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}

@Composable
fun AppText(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    Text(
        modifier = modifier,
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}

@Composable
fun highlightedMatch(text: String, query: String): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(text)
    val matches = matchRangesOf(text = text, needle = needle)
    if (matches.isEmpty()) return AnnotatedString(text)

    val highlight = SpanStyle(background = AppTheme.colors.accentSoft)
    return buildAnnotatedString {
        var cursor = 0
        matches.forEach { start ->
            append(text.substring(cursor, start))
            withStyle(highlight) { append(text.substring(start, start + needle.length)) }
            cursor = start + needle.length
        }
        append(text.substring(cursor))
    }
}

private fun matchRangesOf(text: String, needle: String): List<Int> {
    val starts = mutableListOf<Int>()
    var from = 0
    while (from <= text.length - needle.length) {
        val found = text.indexOf(string = needle, startIndex = from, ignoreCase = true)
        if (found < 0) break
        starts += found
        from = found + needle.length
    }
    return starts
}
