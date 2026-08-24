package app.trainer.base.input

private const val GRAMS_IN_KILOGRAM = 1000
private const val DECIMAL_SEPARATOR_COMMA = ','
private const val DECIMAL_SEPARATOR_DOT = '.'
private const val MAX_FRACTION_DIGITS = 3

class WeightInput {

    fun toGrams(kilogramsText: String): Int? {
        val normalized = kilogramsText.trim().replace(DECIMAL_SEPARATOR_COMMA, DECIMAL_SEPARATOR_DOT)
        if (normalized.isEmpty()) return null

        val parts = normalized.split(DECIMAL_SEPARATOR_DOT)
        if (parts.size > 2) return null

        val wholeKilograms = parts[0].toIntOrNull() ?: return null
        if (wholeKilograms < 0) return null
        if (parts.size == 1) return wholeKilograms * GRAMS_IN_KILOGRAM

        val fraction = parts[1]
        if (fraction.isEmpty() || fraction.length > MAX_FRACTION_DIGITS) return null
        if (!fraction.all(Char::isDigit)) return null

        val fractionGrams = fraction.padEnd(MAX_FRACTION_DIGITS, '0').toInt()
        return wholeKilograms * GRAMS_IN_KILOGRAM + fractionGrams
    }

    fun toKilogramsText(grams: Int): String {
        val wholeKilograms = grams / GRAMS_IN_KILOGRAM
        val fractionGrams = grams % GRAMS_IN_KILOGRAM
        if (fractionGrams == 0) return wholeKilograms.toString()
        val fraction = fractionGrams.toString().padStart(MAX_FRACTION_DIGITS, '0').trimEnd('0')
        return "$wholeKilograms$DECIMAL_SEPARATOR_COMMA$fraction"
    }
}
