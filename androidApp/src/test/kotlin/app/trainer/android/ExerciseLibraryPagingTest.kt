package app.trainer.android

import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseLibraryState
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseRow
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseVideo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

private const val FIRST_CURSOR = "cursor-1"
private const val SECOND_CURSOR = "cursor-2"

class ExerciseLibraryPagingTest {

    @Test
    fun `the first page replaces whatever was shown before`() {
        val state = ExerciseLibraryState.initial()
            .withFirstPage(rows = rows(1, 2, 3), nextCursor = FIRST_CURSOR)

        val reloaded = state.withFirstPage(rows = rows(4), nextCursor = null)

        assertEquals(listOf("exercise-4"), reloaded.exercises.map { it.exerciseId })
        assertFalse(reloaded.hasMore, "сервер не дал курсор — подгружать нечего")
        assertFalse(reloaded.isLoading)
    }

    @Test
    fun `the next page is appended after what is already shown`() {
        val state = ExerciseLibraryState.initial()
            .withFirstPage(rows = rows(1, 2), nextCursor = FIRST_CURSOR)

        val grown = state.withNextPage(rows = rows(3, 4), nextCursor = SECOND_CURSOR)

        assertEquals(
            listOf("exercise-1", "exercise-2", "exercise-3", "exercise-4"),
            grown.exercises.map { it.exerciseId },
        )
        assertEquals(SECOND_CURSOR, grown.nextCursor)
        assertFalse(grown.isLoadingMore, "подгрузка закончилась")
    }

    @Test
    fun `a repeated row from an overlapping page is not shown twice`() {
        val state = ExerciseLibraryState.initial()
            .withFirstPage(rows = rows(1, 2), nextCursor = FIRST_CURSOR)

        val grown = state.withNextPage(rows = rows(2, 3), nextCursor = null)

        assertEquals(listOf("exercise-1", "exercise-2", "exercise-3"), grown.exercises.map { it.exerciseId })
    }

    @Test
    fun `a full first page promises more`() {
        val state = ExerciseLibraryState.initial().withFirstPage(rows = rows(1), nextCursor = FIRST_CURSOR)

        assertTrue(state.hasMore)
    }

    private fun rows(vararg numbers: Int): List<ExerciseRow> = numbers.map { number ->
        ExerciseRow(
            exerciseId = "exercise-$number",
            name = "Упражнение $number",
            details = "Ноги · Сила",
            description = null,
            video = ExerciseVideo.None,
            isOwnedByCoach = true,
            isUploadingVideo = false,
        )
    }
}
