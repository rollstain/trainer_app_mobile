package app.trainer.data.program.impl

import app.trainer.data.program.ClientProgram
import app.trainer.data.program.PlannedWorkout
import app.trainer.data.program.ProgramDay
import app.trainer.data.program.ProgramExerciseLine
import app.trainer.data.program.ProgramSummary
import app.trainer.data.program.TrainingProgram
import app.trainer.logger.Logger
import kotlinx.datetime.LocalDate

private const val LOG_TAG = "program-mapper"

class ProgramMapper(private val logger: Logger) {

    fun toSummary(response: ProgramSummaryResponse): ProgramSummary? {
        val id = response.id ?: return skipped(entity = "ProgramSummary", field = "id")
        val title = response.title ?: return skipped(entity = "ProgramSummary", field = "title")
        val weeksCount = response.weeksCount ?: return skipped(entity = "ProgramSummary", field = "weeksCount")
        return ProgramSummary(
            id = id,
            title = title,
            weeksCount = weeksCount,
            filledDaysCount = response.filledDaysCount ?: 0,
            assignedClientsCount = response.assignedClientsCount ?: 0,
        )
    }

    fun toProgram(response: ProgramResponse): TrainingProgram? {
        val id = response.id ?: return skipped(entity = "TrainingProgram", field = "id")
        val title = response.title ?: return skipped(entity = "TrainingProgram", field = "title")
        val weeksCount = response.weeksCount ?: return skipped(entity = "TrainingProgram", field = "weeksCount")
        return TrainingProgram(
            id = id,
            title = title,
            weeksCount = weeksCount,
            days = response.days.orEmpty().mapNotNull(::toDay),
        )
    }

    fun toPlannedWorkout(response: PlannedWorkoutResponse): PlannedWorkout? {
        val date = parseDate(response.date) ?: return skipped(entity = "PlannedWorkout", field = "date")
        val dayTitle = response.dayTitle ?: return skipped(entity = "PlannedWorkout", field = "dayTitle")
        val weekNumber = response.weekNumber ?: return skipped(entity = "PlannedWorkout", field = "weekNumber")
        return PlannedWorkout(
            date = date,
            programTitle = response.programTitle.orEmpty(),
            dayTitle = dayTitle,
            weekNumber = weekNumber,
            exercises = response.exercises.orEmpty().mapNotNull(::toLine),
        )
    }

    fun toClientProgram(response: ClientProgramResponse): ClientProgram? {
        val programId = response.programId ?: return skipped(entity = "ClientProgram", field = "programId")
        val title = response.programTitle ?: return skipped(entity = "ClientProgram", field = "programTitle")
        val startsOn = parseDate(response.startsOn) ?: return skipped(entity = "ClientProgram", field = "startsOn")
        return ClientProgram(programId = programId, programTitle = title, startsOn = startsOn)
    }

    private fun toDay(response: ProgramDayResponse): ProgramDay? {
        val weekNumber = response.weekNumber ?: return skipped(entity = "ProgramDay", field = "weekNumber")
        val dayOfWeek = response.dayOfWeek ?: return skipped(entity = "ProgramDay", field = "dayOfWeek")
        return ProgramDay(
            weekNumber = weekNumber,
            dayOfWeek = dayOfWeek,
            title = response.title.orEmpty(),
            exercises = response.exercises.orEmpty().mapNotNull(::toLine),
        )
    }

    private fun toLine(response: ProgramExerciseResponse): ProgramExerciseLine? {
        val exerciseId = response.exerciseId ?: return skipped(entity = "ProgramExercise", field = "exerciseId")
        val setsCount = response.setsCount ?: return skipped(entity = "ProgramExercise", field = "setsCount")
        return ProgramExerciseLine(
            exerciseId = exerciseId,
            exerciseName = response.exerciseName.orEmpty(),
            setsCount = setsCount,
            repetitions = response.repetitions,
            weightGrams = response.weightGrams,
            restSeconds = response.restSeconds,
            note = response.note,
        )
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value == null) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }

    private fun <T> skipped(entity: String, field: String): T? {
        logger.error(tag = LOG_TAG, message = "$entity пропущен: нет поля $field")
        return null
    }
}
