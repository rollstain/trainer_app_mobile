package app.trainer.base.diary

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

const val DIARY_LAPSE_THRESHOLD_DAYS = 7

private const val NEW_CLIENT_GRACE_DAYS = 7

sealed interface DiaryLapse {

    data object Logging : DiaryLapse

    data object NotStartedYet : DiaryLapse

    data object NeverLogged : DiaryLapse

    data class Lapsed(val days: Int) : DiaryLapse
}

fun diaryLapseOf(
    today: LocalDate,
    lastEntryDate: LocalDate?,
    linkedDate: LocalDate?,
): DiaryLapse {
    if (lastEntryDate != null) {
        val days = lastEntryDate.daysUntil(today)
        return if (days >= DIARY_LAPSE_THRESHOLD_DAYS) DiaryLapse.Lapsed(days = days) else DiaryLapse.Logging
    }
    if (linkedDate == null) return DiaryLapse.NeverLogged
    return if (linkedDate.daysUntil(today) < NEW_CLIENT_GRACE_DAYS) {
        DiaryLapse.NotStartedYet
    } else {
        DiaryLapse.NeverLogged
    }
}
