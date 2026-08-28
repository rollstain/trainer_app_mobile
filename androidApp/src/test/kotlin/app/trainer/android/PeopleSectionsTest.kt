package app.trainer.android

import app.trainer.feature.clientcard.presentation.people.mvi.PeopleState
import app.trainer.feature.clientcard.presentation.people.mvi.PersonRow
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

private const val FIRST_CURSOR = "cursor-1"
private const val NEXT_SESSION = "пн 01.09 · 10:00"

class PeopleSectionsTest {

    @Test
    fun `booked clients stay in their own section while the rest are paged`() {
        val state = PeopleState.initial().withFirstPage(
            booked = listOf(person("anna", booked = true)),
            others = listOf(person("boris"), person("vera")),
            nextCursor = FIRST_CURSOR,
        )

        assertEquals(listOf("anna"), state.booked.map { it.userId })
        assertEquals(listOf("boris", "vera"), state.others.map { it.userId })
        assertTrue(state.hasMore)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `the next page grows only the section that is paged`() {
        val state = PeopleState.initial().withFirstPage(
            booked = listOf(person("anna", booked = true)),
            others = listOf(person("boris")),
            nextCursor = FIRST_CURSOR,
        )

        val grown = state.withNextPage(rows = listOf(person("vera"), person("gleb")), nextCursor = null)

        assertEquals(listOf("anna"), grown.booked.map { it.userId }, "секция записанных не листается")
        assertEquals(listOf("boris", "vera", "gleb"), grown.others.map { it.userId })
        assertFalse(grown.hasMore)
    }

    @Test
    fun `someone already shown as booked is not repeated among the others`() {
        val state = PeopleState.initial().withFirstPage(
            booked = listOf(person("anna", booked = true)),
            others = listOf(person("boris")),
            nextCursor = FIRST_CURSOR,
        )

        val grown = state.withNextPage(rows = listOf(person("anna"), person("vera")), nextCursor = null)

        assertEquals(listOf("boris", "vera"), grown.others.map { it.userId })
        assertEquals(1, grown.booked.size + grown.others.count { it.userId == "anna" })
    }

    @Test
    fun `an empty roster is empty in both sections`() {
        val state = PeopleState.initial()
            .withFirstPage(booked = emptyList(), others = emptyList(), nextCursor = null)

        assertTrue(state.isEmpty)
        assertFalse(state.hasMore)
        assertFalse(state.isLoading)
    }

    private fun person(userId: String, booked: Boolean = false): PersonRow = PersonRow(
        userId = userId,
        displayName = userId.replaceFirstChar { it.uppercase() },
        hasMedicalNotes = false,
        nextSessionLabel = NEXT_SESSION.takeIf { booked },
        hasPendingChangeRequest = false,
        unreadCount = 0,
        attentionReason = null,
    )
}
