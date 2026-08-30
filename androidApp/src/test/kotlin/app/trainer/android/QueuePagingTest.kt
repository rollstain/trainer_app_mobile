package app.trainer.android

import app.trainer.feature.progress.presentation.checkin.mvi.AwaitingCheckInRow
import app.trainer.feature.progress.presentation.checkin.mvi.CoachCheckInsState
import app.trainer.feature.progress.presentation.formcheck.mvi.AwaitingFormCheck
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachFormChecksState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

private const val FIRST_CURSOR = "cursor-1"
private const val SECOND_CURSOR = "cursor-2"

class QueuePagingTest {

    @Test
    fun `a full page of check-ins promises the rest`() {
        val state = CoachCheckInsState.initial().withFirstPage(rows = checkIns(1, 2), nextCursor = FIRST_CURSOR)

        assertTrue(state.hasMore, "курсор пришёл — очередь не кончилась")
        assertFalse(state.isLoading)
    }

    @Test
    fun `the next page of check-ins is appended after what is shown`() {
        val state = CoachCheckInsState.initial().withFirstPage(rows = checkIns(1, 2), nextCursor = FIRST_CURSOR)

        val grown = state.withNextPage(rows = checkIns(3, 4), nextCursor = SECOND_CURSOR)

        assertEquals(
            listOf("check-in-1", "check-in-2", "check-in-3", "check-in-4"),
            grown.checkIns.map { it.checkInId },
        )
        assertEquals(SECOND_CURSOR, grown.nextCursor)
        assertFalse(grown.isLoadingMore)
    }

    @Test
    fun `a check-in answered between pages is not shown twice`() {
        val state = CoachCheckInsState.initial().withFirstPage(rows = checkIns(1, 2), nextCursor = FIRST_CURSOR)

        val grown = state.withNextPage(rows = checkIns(2, 3), nextCursor = null)

        assertEquals(listOf("check-in-1", "check-in-2", "check-in-3"), grown.checkIns.map { it.checkInId })
        assertFalse(grown.hasMore, "курсора нет — очередь показана целиком")
    }

    @Test
    fun `reloading the awaiting videos replaces the previous page`() {
        val state = CoachFormChecksState.initial().withFirstPage(rows = formChecks(1, 2), nextCursor = FIRST_CURSOR)

        val reloaded = state.withFirstPage(rows = formChecks(3), nextCursor = null)

        assertEquals(listOf("form-check-3"), reloaded.checks.map { it.formCheckId })
        assertFalse(reloaded.hasMore)
    }

    @Test
    fun `a repeated video from an overlapping page is not shown twice`() {
        val state = CoachFormChecksState.initial().withFirstPage(rows = formChecks(1, 2), nextCursor = FIRST_CURSOR)

        val grown = state.withNextPage(rows = formChecks(2, 3), nextCursor = SECOND_CURSOR)

        assertEquals(
            listOf("form-check-1", "form-check-2", "form-check-3"),
            grown.checks.map { it.formCheckId },
        )
    }

    private fun checkIns(vararg numbers: Int): List<AwaitingCheckInRow> = numbers.map { number ->
        AwaitingCheckInRow(
            checkInId = "check-in-$number",
            clientUserId = "client-$number",
            clientDisplayName = "Аня $number",
            dateLabel = "3 марта",
        )
    }

    private fun formChecks(vararg numbers: Int): List<AwaitingFormCheck> = numbers.map { number ->
        AwaitingFormCheck(
            formCheckId = "form-check-$number",
            clientDisplayName = "Аня $number",
            dateLabel = "3 марта",
            exerciseName = null,
            note = null,
            videoUrl = null,
            draft = "",
            isSending = false,
        )
    }
}
