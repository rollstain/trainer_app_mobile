package app.trainer.base.date

import app.trainer.strings.Res
import app.trainer.strings.month_names_genitive
import app.trainer.strings.weekday_labels_short
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import org.jetbrains.compose.resources.getStringArray

private const val TWO_DIGITS = 2

suspend fun monthGenitiveOf(date: LocalDate): String =
    getStringArray(Res.array.month_names_genitive)[date.month.number - 1]

suspend fun weekdayShortOf(date: LocalDate): String =
    getStringArray(Res.array.weekday_labels_short)[date.dayOfWeek.ordinal]

suspend fun weekdayShortOf(ordinal: Int): String =
    getStringArray(Res.array.weekday_labels_short)[ordinal]

fun timeOfDayOf(dateTime: LocalDateTime): String =
    "${twoDigits(dateTime.hour)}:${twoDigits(dateTime.minute)}"

fun dayMonthOf(date: LocalDate): String =
    "${twoDigits(date.day)}.${twoDigits(date.month.number)}"

fun dayMonthYearOf(date: LocalDate): String = "${dayMonthOf(date)}.${date.year}"

private fun twoDigits(value: Int): String = value.toString().padStart(TWO_DIGITS, '0')
