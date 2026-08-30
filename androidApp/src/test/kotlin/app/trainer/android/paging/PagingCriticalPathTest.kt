package app.trainer.android.paging

import app.trainer.android.auth.MockBackend
import app.trainer.android.auth.SilentLogger
import app.trainer.data.clients.impl.ClientsMapper
import app.trainer.data.clients.impl.ParticipantsRepositoryImpl
import app.trainer.data.program.impl.ProgramMapper
import app.trainer.data.program.impl.ProgramRepositoryImpl
import app.trainer.data.progress.impl.CheckInRepositoryImpl
import app.trainer.data.progress.impl.FormCheckRepositoryImpl
import app.trainer.data.progress.impl.ProgressMapper
import app.trainer.data.schedule.impl.ScheduleMapper
import app.trainer.data.schedule.impl.ScheduleRepositoryImpl
import app.trainer.entities.RequestResult
import app.trainer.network.PresignedUploader
import io.ktor.http.HttpStatusCode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val NEXT_CURSOR_HEADER = "X-Next-Cursor"
private const val FIRST_CURSOR = "cursor-page-2"
private const val PAGE_SIZE = 20

private const val AWAITING_CHECK_INS_FIRST_PAGE = """
[
  {"checkInId":"check-in-1","clientUserId":"client-1","clientDisplayName":"Анна","checkInDate":"2026-03-01"},
  {"checkInId":"check-in-2","clientUserId":"client-2","clientDisplayName":"Вера","checkInDate":"2026-02-28"}
]
"""

private const val AWAITING_CHECK_INS_LAST_PAGE = """
[{"checkInId":"check-in-3","clientUserId":"client-3","clientDisplayName":"Глеб","checkInDate":"2026-02-27"}]
"""

private const val FORM_CHECKS_FIRST_PAGE = """
[
  {
    "id":"form-check-1","clientUserId":"client-1","clientDisplayName":"Анна","exerciseId":null,
    "exerciseName":null,"video":null,"note":null,"coachComment":null,"isReviewed":false,
    "createdAt":"2026-03-02T09:00:00Z"
  }
]
"""

private const val PROGRAMS_FIRST_PAGE = """
[{"id":"program-1","title":"Набор массы","weeksCount":4,"filledDaysCount":3,"assignedClientsCount":2}]
"""

private const val CHANGE_REQUESTS_PAGE = """
[
  {
    "id":"request-1","slotId":"slot-1","slotStartsAt":"2026-03-03T09:00:00Z","proposedStartsAt":null,
    "kind":"CANCEL","status":"PENDING","requestedByUserId":"client-1","requestedByDisplayName":"Анна",
    "createdAt":"2026-03-02T08:00:00Z"
  }
]
"""

private const val MISSED_SESSIONS = """
[{"clientUserId":"client-1","missedInARow":2},{"clientUserId":"client-2","missedInARow":3}]
"""

class PagingCriticalPathTest {

    private val backend = MockBackend()

    private val checkIns = CheckInRepositoryImpl(
        httpClientProvider = backend.httpClientProvider,
        mapper = ProgressMapper(logger = SilentLogger),
        presignedUploader = RefusingUploader,
    )

    private val formChecks = FormCheckRepositoryImpl(
        httpClientProvider = backend.httpClientProvider,
        mapper = ProgressMapper(logger = SilentLogger),
        presignedUploader = RefusingUploader,
        logger = SilentLogger,
    )

    private val programs = ProgramRepositoryImpl(
        httpClientProvider = backend.httpClientProvider,
        mapper = ProgramMapper(logger = SilentLogger),
    )

    private val schedule = ScheduleRepositoryImpl(
        httpClientProvider = backend.httpClientProvider,
        mapper = ScheduleMapper(logger = SilentLogger),
    )

    private val participants = ParticipantsRepositoryImpl(
        httpClientProvider = backend.httpClientProvider,
        mapper = ClientsMapper(logger = SilentLogger),
    )

    @Test
    fun `the awaiting check-ins queue asks for a page and reads the cursor from the header`() = runTest {
        backend.on(
            method = "GET",
            path = "/coach/check-ins/awaiting",
            status = HttpStatusCode.OK,
            body = AWAITING_CHECK_INS_FIRST_PAGE,
            headers = mapOf(NEXT_CURSOR_HEADER to FIRST_CURSOR),
        )

        val page = checkIns.awaitingReview(limit = PAGE_SIZE, after = null)

        val loaded = (page as RequestResult.Success).data
        assertEquals(listOf("check-in-1", "check-in-2"), loaded.items.map { it.checkInId })
        assertEquals(FIRST_CURSOR, loaded.nextCursor)
        assertTrue(loaded.hasMore)
        assertEquals(listOf("limit=$PAGE_SIZE"), backend.queriesOf("GET", "/coach/check-ins/awaiting"))
    }

    @Test
    fun `the next page of check-ins is asked for after the cursor`() = runTest {
        backend.on(
            method = "GET",
            path = "/coach/check-ins/awaiting",
            status = HttpStatusCode.OK,
            body = AWAITING_CHECK_INS_LAST_PAGE,
        )

        val page = checkIns.awaitingReview(limit = PAGE_SIZE, after = FIRST_CURSOR)

        val loaded = (page as RequestResult.Success).data
        assertEquals(listOf("check-in-3"), loaded.items.map { it.checkInId })
        assertNull(loaded.nextCursor, "заголовка нет — очередь кончилась")
        assertEquals(
            listOf("limit=$PAGE_SIZE&after=$FIRST_CURSOR"),
            backend.queriesOf("GET", "/coach/check-ins/awaiting"),
        )
    }

    @Test
    fun `the awaiting videos queue is paged the same way`() = runTest {
        backend.on(
            method = "GET",
            path = "/coach/form-checks/awaiting",
            status = HttpStatusCode.OK,
            body = FORM_CHECKS_FIRST_PAGE,
            headers = mapOf(NEXT_CURSOR_HEADER to FIRST_CURSOR),
        )

        val page = formChecks.awaitingReview(limit = PAGE_SIZE, after = null)

        val loaded = (page as RequestResult.Success).data
        assertEquals(listOf("form-check-1"), loaded.items.map { it.id })
        assertEquals(FIRST_CURSOR, loaded.nextCursor)
    }

    @Test
    fun `own video history is paged too`() = runTest {
        backend.on(
            method = "GET",
            path = "/me/form-checks",
            status = HttpStatusCode.OK,
            body = FORM_CHECKS_FIRST_PAGE,
        )

        val page = formChecks.ownFormChecks(limit = PAGE_SIZE, after = FIRST_CURSOR)

        assertTrue(page is RequestResult.Success)
        assertEquals(listOf("limit=$PAGE_SIZE&after=$FIRST_CURSOR"), backend.queriesOf("GET", "/me/form-checks"))
    }

    @Test
    fun `programs come by pages`() = runTest {
        backend.on(
            method = "GET",
            path = "/coach/programs",
            status = HttpStatusCode.OK,
            body = PROGRAMS_FIRST_PAGE,
            headers = mapOf(NEXT_CURSOR_HEADER to FIRST_CURSOR),
        )

        val page = programs.programs(limit = PAGE_SIZE, after = null)

        val loaded = (page as RequestResult.Success).data
        assertEquals(listOf("program-1"), loaded.items.map { it.id })
        assertEquals(FIRST_CURSOR, loaded.nextCursor)
    }

    @Test
    fun `the schedule asks for change requests of the week it shows`() = runTest {
        backend.on(
            method = "GET",
            path = "/schedule/change-requests/pending",
            status = HttpStatusCode.OK,
            body = CHANGE_REQUESTS_PAGE,
        )

        val loaded = schedule.pendingChangeRequestsBetween(
            from = Instant.parse("2026-03-02T00:00:00Z"),
            to = Instant.parse("2026-03-09T00:00:00Z"),
        )

        assertEquals(listOf("request-1"), (loaded as RequestResult.Success).data.map { it.id })
        assertEquals(
            listOf("from=2026-03-02T00%3A00%3A00Z&to=2026-03-09T00%3A00%3A00Z"),
            backend.queriesOf("GET", "/schedule/change-requests/pending"),
        )
    }

    @Test
    fun `the queue of change requests can be read page by page`() = runTest {
        backend.on(
            method = "GET",
            path = "/schedule/change-requests/pending",
            status = HttpStatusCode.OK,
            body = CHANGE_REQUESTS_PAGE,
            headers = mapOf(NEXT_CURSOR_HEADER to FIRST_CURSOR),
        )

        val page = schedule.pendingChangeRequests(limit = PAGE_SIZE, after = null)

        val loaded = (page as RequestResult.Success).data
        assertEquals(listOf("request-1"), loaded.items.map { it.id })
        assertEquals(FIRST_CURSOR, loaded.nextCursor)
    }

    @Test
    fun `missed sessions are asked only for the clients on the screen`() = runTest {
        backend.on(
            method = "GET",
            path = "/coach/clients/missed-sessions",
            status = HttpStatusCode.OK,
            body = MISSED_SESSIONS,
        )

        val loaded = participants.missedSessions(listOf("client-1", "client-2"))

        assertEquals(mapOf("client-1" to 2, "client-2" to 3), (loaded as RequestResult.Success).data)
        assertEquals(
            listOf("clientUserIds=client-1&clientUserIds=client-2"),
            backend.queriesOf("GET", "/coach/clients/missed-sessions"),
        )
    }

    @Test
    fun `an empty roster page is not even asked about missed sessions`() = runTest {
        val loaded = participants.missedSessions(emptyList())

        assertEquals(emptyMap(), (loaded as RequestResult.Success).data)
        assertTrue(backend.calls.isEmpty(), "без людей на экране запрос не нужен")
    }
}

internal object RefusingUploader : PresignedUploader {

    override suspend fun upload(
        uploadUrl: String,
        contentType: String,
        bytes: ByteArray,
    ): RequestResult<Unit> = error("Загрузка файлов в этом тесте не участвует")
}
