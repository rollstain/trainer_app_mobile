package app.trainer.android.auth

import app.trainer.data.auth.impl.AuthRepositoryImpl
import app.trainer.data.auth.impl.LoginMethodsRepositoryImpl
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import io.ktor.http.HttpStatusCode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val DEVICE = "Pixel 8"
private const val EMAIL = "anna@mail.ru"
private const val LOGIN = "anna_k"
private const val PASSWORD = "gantel-2026"
private const val TOKENS = """{"accessToken":"access-1","refreshToken":"refresh-1"}"""

class AuthCriticalPathTest {

    private val backend = MockBackend()
    private val tokens = RecordingTokenStorage()
    private val events = RecordingSessionEvents()

    private val auth = AuthRepositoryImpl(
        httpClientProvider = backend.httpClientProvider,
        tokenStorage = tokens,
        sessionEvents = events,
        logger = SilentLogger,
    )

    private val loginMethods = LoginMethodsRepositoryImpl(
        httpClientProvider = backend.httpClientProvider,
        logger = SilentLogger,
    )

    @Test
    fun `sign-up keeps the session and tells the app the user changed`() = runTest {
        backend.on("POST", "/auth/password/sign-up", HttpStatusCode.OK, TOKENS)

        val signedUp = auth.signUpWithPassword(
            displayName = " Анна ",
            email = " Anna@Mail.ru ",
            login = LOGIN,
            password = PASSWORD,
            deviceInfo = DEVICE,
        )

        assertTrue(signedUp is RequestResult.Success)
        assertEquals("access-1", tokens.tokens?.accessToken)
        assertEquals(1, events.authChanges.size)
        val sent = backend.bodyOf("POST", "/auth/password/sign-up")
        assertTrue(sent.contains("\"displayName\":\"Анна\""), "имя уходит без лишних пробелов: $sent")
        assertTrue(sent.contains("\"email\":\"Anna@Mail.ru\""), "почта уходит как введена: $sent")
    }

    @Test
    fun `a taken email says which field is to blame`() = runTest {
        backend.on(
            method = "POST",
            path = "/auth/password/sign-up",
            status = HttpStatusCode.Conflict,
            body = """{"status":409,"message":"Эта почта уже занята","fieldErrors":{"email":"Эта почта уже занята"}}""",
        )

        val rejected = auth.signUpWithPassword(
            displayName = "Анна",
            email = EMAIL,
            login = null,
            password = PASSWORD,
            deviceInfo = DEVICE,
        )

        val error = rejected as RequestResult.Error
        assertEquals(RequestFailure.Conflict, error.kind)
        assertEquals(setOf("email"), error.fieldErrors.keys)
        assertNull(tokens.tokens, "провалившаяся регистрация не должна оставлять сессию")
    }

    @Test
    fun `sign-in works by login and stores the session`() = runTest {
        backend.on("POST", "/auth/password/sign-in", HttpStatusCode.OK, TOKENS)

        val signedIn = auth.signInWithPassword(identifier = LOGIN, password = PASSWORD, deviceInfo = DEVICE)

        assertTrue(signedIn is RequestResult.Success)
        assertEquals("refresh-1", tokens.tokens?.refreshToken)
        assertTrue(backend.bodyOf("POST", "/auth/password/sign-in").contains("\"identifier\":\"$LOGIN\""))
    }

    @Test
    fun `a wrong pair leaves no session and reads as unauthorized`() = runTest {
        backend.on(
            method = "POST",
            path = "/auth/password/sign-in",
            status = HttpStatusCode.Unauthorized,
            body = """{"status":401,"message":"Неверная почта, логин или пароль","fieldErrors":{}}""",
        )

        val rejected = auth.signInWithPassword(identifier = EMAIL, password = "wrong", deviceInfo = DEVICE)

        assertEquals(RequestFailure.Unauthorized, (rejected as RequestResult.Error).kind)
        assertNull(tokens.tokens)
    }

    @Test
    fun `a locked account carries the countdown the screen shows`() = runTest {
        backend.on(
            method = "POST",
            path = "/auth/password/sign-in",
            status = HttpStatusCode.TooManyRequests,
            body = """{"status":429,"message":"Вход закрыт","fieldErrors":{},"retryAfterSeconds":300}""",
            headers = mapOf("Retry-After" to "300"),
        )

        val locked = auth.signInWithPassword(identifier = EMAIL, password = "wrong", deviceInfo = DEVICE)

        val error = locked as RequestResult.Error
        assertEquals(RequestFailure.TooManyRequests, error.kind)
        assertEquals(300L, error.retryAfterSeconds)
    }

    @Test
    fun `asking for a letter hits forgot and keeps no session`() = runTest {
        backend.on("POST", "/auth/password/forgot", HttpStatusCode.NoContent, "")

        val asked = auth.requestPasswordReset(email = EMAIL)

        assertTrue(asked is RequestResult.Success)
        assertEquals(listOf("POST /auth/password/forgot"), backend.pathsCalled())
        assertNull(tokens.tokens)
    }

    @Test
    fun `a refused letter comes back as a server failure, not silence`() = runTest {
        backend.on(
            method = "POST",
            path = "/auth/password/forgot",
            status = HttpStatusCode.InternalServerError,
            body = """{"status":500,"message":"Что-то пошло не так","fieldErrors":{}}""",
        )

        val asked = auth.requestPasswordReset(email = EMAIL)

        assertEquals(RequestFailure.Server, (asked as RequestResult.Error).kind)
    }

    @Test
    fun `the link from the letter opens a session`() = runTest {
        backend.on("POST", "/auth/password/reset/email", HttpStatusCode.OK, TOKENS)

        val reset = auth.resetPasswordByEmail(token = "link-token", password = PASSWORD, deviceInfo = DEVICE)

        assertTrue(reset is RequestResult.Success)
        assertEquals("access-1", tokens.tokens?.accessToken)
        assertTrue(backend.bodyOf("POST", "/auth/password/reset/email").contains("\"token\":\"link-token\""))
    }

    @Test
    fun `a used link and an expired link are told apart`() = runTest {
        backend.on(
            method = "POST",
            path = "/auth/password/reset/email",
            status = HttpStatusCode.Conflict,
            body = """{"status":409,"message":"Ссылка уже использована","fieldErrors":{}}""",
        )
        backend.on(
            method = "POST",
            path = "/auth/password/reset/email",
            status = HttpStatusCode.Gone,
            body = """{"status":410,"message":"Срок ссылки истёк","fieldErrors":{}}""",
        )

        val used = auth.resetPasswordByEmail(token = "used", password = PASSWORD, deviceInfo = DEVICE)
        val expired = auth.resetPasswordByEmail(token = "old", password = PASSWORD, deviceInfo = DEVICE)

        assertEquals(RequestFailure.Conflict, (used as RequestResult.Error).kind)
        assertEquals(RequestFailure.Gone, (expired as RequestResult.Error).kind)
    }

    @Test
    fun `telegram reset goes to its own endpoint`() = runTest {
        backend.on("POST", "/auth/password/reset/telegram", HttpStatusCode.OK, TOKENS)

        val reset = auth.resetPasswordByTelegram(claimToken = "claim", password = PASSWORD, deviceInfo = DEVICE)

        assertTrue(reset is RequestResult.Success)
        assertEquals(listOf("POST /auth/password/reset/telegram"), backend.pathsCalled())
    }

    @Test
    fun `setting a password in the profile sends only what was filled`() = runTest {
        backend.on("PUT", "/me/password", HttpStatusCode.OK, "")

        val saved = loginMethods.setPassword(
            email = " anna@mail.ru ",
            login = null,
            currentPassword = null,
            newPassword = PASSWORD,
        )

        assertTrue(saved is RequestResult.Success)
        val sent = backend.bodyOf("PUT", "/me/password")
        assertTrue(sent.contains("\"email\":\"anna@mail.ru\""), "почта уходит обрезанной: $sent")
        assertTrue(sent.contains("\"newPassword\":\"$PASSWORD\""), sent)
        assertTrue(!sent.contains("\"login\":\"\""), "пустой логин не отправляется: $sent")
    }

    @Test
    fun `a wrong current password is a forbidden, not a sign-out`() = runTest {
        backend.on(
            method = "PUT",
            path = "/me/password",
            status = HttpStatusCode.Forbidden,
            body = """{"status":403,"message":"Текущий пароль неверен","fieldErrors":{}}""",
        )

        val rejected = loginMethods.setPassword(
            email = null,
            login = null,
            currentPassword = "not-mine",
            newPassword = PASSWORD,
        )

        assertEquals(RequestFailure.Forbidden, (rejected as RequestResult.Error).kind)
    }

    @Test
    fun `a confirm link goes to the public endpoint and opens no session`() = runTest {
        backend.on("POST", "/auth/email/confirm", HttpStatusCode.NoContent, "")

        val confirmed = loginMethods.confirmEmail(token = "letter-token")

        assertTrue(confirmed is RequestResult.Success)
        assertEquals(listOf("POST /auth/email/confirm"), backend.pathsCalled())
        assertTrue(backend.bodyOf("POST", "/auth/email/confirm").contains("\"token\":\"letter-token\""))
        assertNull(tokens.tokens, "подтверждение почты — не вход")
    }

    @Test
    fun `a used confirm link and an expired one are told apart`() = runTest {
        backend.on(
            method = "POST",
            path = "/auth/email/confirm",
            status = HttpStatusCode.Conflict,
            body = """{"status":409,"message":"Ссылка уже использована","fieldErrors":{}}""",
        )
        backend.on(
            method = "POST",
            path = "/auth/email/confirm",
            status = HttpStatusCode.Gone,
            body = """{"status":410,"message":"Срок ссылки истёк","fieldErrors":{}}""",
        )

        val used = loginMethods.confirmEmail(token = "used")
        val expired = loginMethods.confirmEmail(token = "old")

        assertEquals(RequestFailure.Conflict, (used as RequestResult.Error).kind)
        assertEquals(RequestFailure.Gone, (expired as RequestResult.Error).kind)
    }

    @Test
    fun `asking for another confirm letter respects the cooldown`() = runTest {
        backend.on("POST", "/me/email/confirm-request", HttpStatusCode.NoContent, "")
        backend.on(
            method = "POST",
            path = "/me/email/confirm-request",
            status = HttpStatusCode.TooManyRequests,
            body = """{"status":429,"message":"Письмо уже отправлено","fieldErrors":{},"retryAfterSeconds":90}""",
            headers = mapOf("Retry-After" to "90"),
        )

        val sent = loginMethods.requestEmailConfirmation()
        val tooSoon = loginMethods.requestEmailConfirmation()

        assertTrue(sent is RequestResult.Success)
        val error = tooSoon as RequestResult.Error
        assertEquals(RequestFailure.TooManyRequests, error.kind)
        assertEquals(90L, error.retryAfterSeconds)
    }

    @Test
    fun `logout drops the session`() = runTest {
        backend.on("POST", "/auth/password/sign-in", HttpStatusCode.OK, TOKENS)
        auth.signInWithPassword(identifier = EMAIL, password = PASSWORD, deviceInfo = DEVICE)

        auth.logout()

        assertNull(tokens.tokens)
    }
}
