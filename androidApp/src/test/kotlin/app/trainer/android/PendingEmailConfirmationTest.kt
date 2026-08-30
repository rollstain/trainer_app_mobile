package app.trainer.android

import app.trainer.app.PendingEmailConfirmation
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

private const val TOKEN = "confirm-token-43-symbols-like-the-real-one"

class PendingEmailConfirmationTest {

    @Test
    fun `a web link carries the token`() {
        val confirmation = PendingEmailConfirmation()

        confirmation.remember("https://api.lyashukfit.ru/c/$TOKEN")

        assertEquals(TOKEN, confirmation.token.value)
    }

    @Test
    fun `an app link carries the token too`() {
        val confirmation = PendingEmailConfirmation()

        confirmation.remember("trainer://confirm/$TOKEN")

        assertEquals(TOKEN, confirmation.token.value)
    }

    @Test
    fun `a reset link is not a confirmation`() {
        val confirmation = PendingEmailConfirmation()

        confirmation.remember("https://api.lyashukfit.ru/r/$TOKEN")

        assertNull(confirmation.token.value)
    }

    @Test
    fun `a handled link stops being pending`() {
        val confirmation = PendingEmailConfirmation()
        confirmation.remember("trainer://confirm/$TOKEN")

        confirmation.consume()

        assertNull(confirmation.token.value)
    }
}
