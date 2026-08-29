package app.trainer.uikit.widgets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import app.trainer.uikit.AppTheme
import kotlinx.collections.immutable.ImmutableList

data class LegalLink(val label: String, val url: String)

@Composable
fun AppLegalNote(
    modifier: Modifier = Modifier,
    text: String,
    links: ImmutableList<LegalLink>,
) {
    val underlined = TextLinkStyles(
        style = SpanStyle(
            color = AppTheme.colors.textSecondary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val annotated = buildAnnotatedString {
        append(text)
        links.forEach { link ->
            val start = text.indexOf(link.label)
            if (start < 0) return@forEach
            addLink(
                url = LinkAnnotation.Url(url = link.url, styles = underlined),
                start = start,
                end = start + link.label.length,
            )
        }
    }
    Text(
        modifier = modifier,
        text = annotated,
        style = AppTheme.typography.caption,
        color = AppTheme.colors.textMuted,
        textAlign = TextAlign.Center,
    )
}
