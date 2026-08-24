package app.trainer.feature.traininglog.domain

private const val SECONDS_IN_MINUTE = 60

class DurationInput {

    fun toSeconds(minutesText: String): Int? {
        val minutes = minutesText.trim().toIntOrNull() ?: return null
        if (minutes < 0) return null
        return minutes * SECONDS_IN_MINUTE
    }

    fun toMinutesText(seconds: Int): String = (seconds / SECONDS_IN_MINUTE).toString()
}
