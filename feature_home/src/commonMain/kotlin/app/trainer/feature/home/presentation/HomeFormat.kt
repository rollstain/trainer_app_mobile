package app.trainer.feature.home.presentation

import app.trainer.strings.Res
import app.trainer.strings.home_starts_in_days
import app.trainer.strings.home_starts_in_hours
import app.trainer.strings.home_starts_in_minutes
import app.trainer.strings.home_starts_now
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString

private const val MINUTES_IN_HOUR = 60
private const val HOURS_IN_DAY = 24

internal suspend fun startsInLabelOf(startsAt: Instant, now: Instant): String {
    val minutes = (startsAt - now).inWholeMinutes.toInt()
    if (minutes <= 0) return getString(Res.string.home_starts_now)
    val hours = minutes / MINUTES_IN_HOUR
    val days = hours / HOURS_IN_DAY
    return when {
        days > 0 -> getString(Res.string.home_starts_in_days, days)
        hours > 0 -> getString(Res.string.home_starts_in_hours, hours)
        else -> getString(Res.string.home_starts_in_minutes, minutes)
    }
}
