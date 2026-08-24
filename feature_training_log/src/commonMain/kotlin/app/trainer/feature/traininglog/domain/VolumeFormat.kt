package app.trainer.feature.traininglog.domain

private const val GRAMS_IN_KILOGRAM = 1_000L
private const val GRAMS_IN_TON = 1_000_000L
private const val TON_FRACTION_STEP = 100_000L
private const val THOUSAND = 1000L
private const val NON_BREAKING_SPACE = '\u00A0'
private const val DECIMAL_SEPARATOR = ','

class VolumeFormat {

    fun toKilograms(grams: Long): String {
        val kilograms = grams / GRAMS_IN_KILOGRAM
        return "${withThousandSeparators(kilograms)}${NON_BREAKING_SPACE}кг"
    }

    fun toTons(grams: Long): String {
        val wholeTons = grams / GRAMS_IN_TON
        val fraction = (grams % GRAMS_IN_TON) / TON_FRACTION_STEP
        return "${withThousandSeparators(wholeTons)}$DECIMAL_SEPARATOR$fraction${NON_BREAKING_SPACE}т"
    }

    private fun withThousandSeparators(value: Long): String {
        val digits = value.toString()
        if (value < THOUSAND) return digits
        return digits
            .reversed()
            .chunked(size = 3)
            .joinToString(separator = NON_BREAKING_SPACE.toString())
            .reversed()
    }
}
