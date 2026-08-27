package app.trainer.base.format

import app.trainer.strings.Res
import app.trainer.strings.volume_kilograms
import app.trainer.strings.volume_tons
import org.jetbrains.compose.resources.getString

private const val GRAMS_IN_KILOGRAM = 1_000L
private const val GRAMS_IN_TON = 1_000_000L
private const val TON_FRACTION_STEP = 100_000L
private const val THOUSAND = 1000L
private const val NON_BREAKING_SPACE = '\u00A0'
private const val DECIMAL_SEPARATOR = ','

class VolumeFormat {

    suspend fun toKilograms(grams: Long): String {
        val kilograms = grams / GRAMS_IN_KILOGRAM
        return getString(Res.string.volume_kilograms, withThousandSeparators(kilograms))
    }

    suspend fun toTons(grams: Long): String {
        val wholeTons = grams / GRAMS_IN_TON
        val fraction = (grams % GRAMS_IN_TON) / TON_FRACTION_STEP
        return getString(
            Res.string.volume_tons,
            "${withThousandSeparators(wholeTons)}$DECIMAL_SEPARATOR$fraction",
        )
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
