package app.trainer.android

import app.trainer.data.traininglog.ExerciseKind
import app.trainer.feature.traininglog.presentation.editor.mvi.LastResultHints
import app.trainer.feature.traininglog.presentation.editor.mvi.SetRow
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

private const val LAST_WEIGHT = "62,5"
private const val LAST_REPETITIONS = "8"
private const val TYPED_WEIGHT = "65"

class SetRowCountTest {

    @Test
    fun `counting an untouched set accepts what was lifted last time`() {
        val counted = strengthRow().counted()

        assertEquals(LAST_WEIGHT, counted.weightText)
        assertEquals(LAST_REPETITIONS, counted.repetitionsText)
        assertTrue(counted.isCounted)
    }

    @Test
    fun `counting never overwrites what the person typed`() {
        val counted = strengthRow().copy(weightText = TYPED_WEIGHT).counted()

        assertEquals(TYPED_WEIGHT, counted.weightText, "введённый вес важнее подсказки")
        assertEquals(LAST_REPETITIONS, counted.repetitionsText, "пустое поле берёт подсказку")
    }

    @Test
    fun `a set without a hint stays empty and still counts`() {
        val counted = strengthRow(hints = LastResultHints.Empty).counted()

        assertTrue(counted.weightText.isEmpty(), "подставлять нечего")
        assertTrue(counted.isCounted, "подход всё равно зачтён")
    }

    @Test
    fun `an untouched set is empty and a typed one is not`() {
        assertTrue(strengthRow().isEmpty)
        assertFalse(strengthRow().copy(repetitionsText = "10").isEmpty)
    }

    private fun strengthRow(
        hints: LastResultHints = LastResultHints(
            repetitions = LAST_REPETITIONS,
            weight = LAST_WEIGHT,
            duration = "",
            distance = "",
        ),
    ): SetRow = SetRow(
        rowId = "row-1",
        exerciseId = "exercise-1",
        exerciseName = "Жим штанги лёжа",
        kind = ExerciseKind.STRENGTH,
        repetitionsText = "",
        weightText = "",
        durationText = "",
        distanceText = "",
        lastResult = hints,
        plannedRestSeconds = null,
        isPersonalRecord = false,
        isCounted = false,
    )
}
