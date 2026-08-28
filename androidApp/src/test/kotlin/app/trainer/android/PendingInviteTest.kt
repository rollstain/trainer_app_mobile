package app.trainer.android

import app.trainer.app.PendingInvite
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

private const val CODE = "CVSKQJ"

class PendingInviteTest {

    @Test
    fun `a web link carries the code`() {
        val invite = PendingInvite()

        invite.remember("https://lyashukfit.ru/i/$CODE")

        assertEquals(CODE, invite.code.value)
    }

    @Test
    fun `an app link carries the code too`() {
        val invite = PendingInvite()

        invite.remember("trainer://invite/$CODE")

        assertEquals(CODE, invite.code.value)
    }

    @Test
    fun `a lowercase code arrives as the server stores it`() {
        val invite = PendingInvite()

        invite.remember("https://lyashukfit.ru/i/${CODE.lowercase()}")

        assertEquals(CODE, invite.code.value)
    }

    @Test
    fun `tracking parameters do not become part of the code`() {
        val invite = PendingInvite()

        invite.remember("https://lyashukfit.ru/i/$CODE?utm_source=telegram")

        assertEquals(CODE, invite.code.value)
    }

    @Test
    fun `a link that is not an invitation is ignored`() {
        val invite = PendingInvite()

        invite.remember("https://lyashukfit.ru/about")

        assertNull(invite.code.value)
    }

    @Test
    fun `a used invitation stops being pending`() {
        val invite = PendingInvite()
        invite.remember("https://lyashukfit.ru/i/$CODE")

        invite.consume()

        assertNull(invite.code.value)
    }
}
