package app.trainer.entities

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

data class WorkingDay(
    val dayOfWeek: DayOfWeek,
    val opensAt: LocalTime,
    val closesAt: LocalTime,
)
