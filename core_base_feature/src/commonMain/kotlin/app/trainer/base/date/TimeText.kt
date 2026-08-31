package app.trainer.base.date

import kotlinx.datetime.LocalTime

const val TIME_DIGITS_LENGTH = 4

private const val HOUR_DIGITS = 2
private const val TIME_SEPARATOR = ':'
private const val MAX_HOUR_TENS = '2'
private const val MAX_HOUR_UNIT_AFTER_TWO = '3'
private const val MAX_MINUTE_TENS = '5'
private const val ZERO = '0'

fun filterTimeDigits(raw: String): String {
    val digits = StringBuilder()
    for (symbol in raw.filter(Char::isDigit)) {
        when (digits.length) {
            0 -> if (symbol <= MAX_HOUR_TENS) {
                digits.append(symbol)
            } else {
                digits.append(ZERO).append(symbol)
            }
            1 -> if (digits[0] < MAX_HOUR_TENS || symbol <= MAX_HOUR_UNIT_AFTER_TWO) digits.append(symbol)
            2 -> if (symbol <= MAX_MINUTE_TENS) digits.append(symbol)
            else -> digits.append(symbol)
        }
        if (digits.length == TIME_DIGITS_LENGTH) break
    }
    return digits.toString()
}

fun formatTimeDigits(digits: String): String = if (digits.length <= HOUR_DIGITS) {
    digits
} else {
    digits.take(HOUR_DIGITS) + TIME_SEPARATOR + digits.drop(HOUR_DIGITS)
}

fun parseTimeDigits(digits: String): LocalTime? {
    if (digits.length != TIME_DIGITS_LENGTH) return null
    val hours = digits.take(HOUR_DIGITS).toIntOrNull() ?: return null
    val minutes = digits.drop(HOUR_DIGITS).toIntOrNull() ?: return null
    return runCatching { LocalTime(hour = hours, minute = minutes) }.getOrNull()
}

fun timeDigitsOf(time: LocalTime): String = time.toString().filter(Char::isDigit).take(TIME_DIGITS_LENGTH)
