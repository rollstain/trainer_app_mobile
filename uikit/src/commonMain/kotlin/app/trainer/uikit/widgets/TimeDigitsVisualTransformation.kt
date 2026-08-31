package app.trainer.uikit.widgets

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

private const val HOUR_DIGITS = 2
private const val TIME_SEPARATOR = ":"

object TimeDigitsVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val shown = if (digits.length <= HOUR_DIGITS) {
            digits
        } else {
            digits.take(HOUR_DIGITS) + TIME_SEPARATOR + digits.drop(HOUR_DIGITS)
        }
        val mapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int =
                if (digits.length <= HOUR_DIGITS || offset <= HOUR_DIGITS) offset else offset + 1

            override fun transformedToOriginal(offset: Int): Int =
                if (digits.length <= HOUR_DIGITS || offset <= HOUR_DIGITS) offset else offset - 1
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}
