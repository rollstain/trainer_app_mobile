package app.trainer.base.date

import kotlinx.datetime.LocalTime

const val TIME_TEXT_LENGTH = 5

private const val TIME_SEPARATOR = ':'
private const val HOUR_DIGITS = 2
private const val MINUTE_DIGITS = 2
private const val TIME_PARTS = 2
private const val MINUTES_IN_HOUR = 60
private const val LAST_HOUR = 23

fun normalizeTimeText(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(HOUR_DIGITS + MINUTE_DIGITS)
    return when {
        digits.length <= HOUR_DIGITS -> digits
        else -> "${digits.take(HOUR_DIGITS)}$TIME_SEPARATOR${digits.drop(HOUR_DIGITS)}"
    }
}

fun parseTimeText(text: String): LocalTime? {
    val parts = text.split(TIME_SEPARATOR)
    if (parts.size != TIME_PARTS) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..LAST_HOUR || minutes !in 0 until MINUTES_IN_HOUR) return null
    return LocalTime(hour = hours, minute = minutes)
}
