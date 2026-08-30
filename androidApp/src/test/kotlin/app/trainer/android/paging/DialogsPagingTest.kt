package app.trainer.android.paging

import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.trainer.android.auth.MockBackend
import app.trainer.android.auth.SilentLogger
import app.trainer.data.chat.impl.ChatLocalStore
import app.trainer.data.chat.impl.ChatMapper
import app.trainer.data.chat.impl.ChatRepositoryImpl
import app.trainer.database.TrainerDatabase
import app.trainer.entities.RequestResult
import io.ktor.http.HttpStatusCode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val NEXT_CURSOR_HEADER = "X-Next-Cursor"
private const val FIRST_CURSOR = "cursor-dialogs-2"
private const val DIALOGS_PAGE_SIZE = 30
private const val ROBOLECTRIC_SDK = 34

private const val DIALOGS_FIRST_PAGE = """
[
  {
    "id":"dialog-1","coachId":"coach-1","clientUserId":"client-1","peerUserId":"client-1",
    "peerDisplayName":"Анна","lastMessageSeq":7,"readSeq":5,"peerReadSeq":7,"unreadCount":2,
    "lastMessagePreview":"Готова к завтрашней","lastMessageAt":"2026-03-02T09:00:00Z"
  },
  {
    "id":"dialog-2","coachId":"coach-1","clientUserId":"client-2","peerUserId":"client-2",
    "peerDisplayName":"Вера","lastMessageSeq":3,"readSeq":3,"peerReadSeq":3,"unreadCount":0,
    "lastMessagePreview":"Спасибо!","lastMessageAt":"2026-03-01T09:00:00Z"
  }
]
"""

private const val DIALOGS_SECOND_PAGE = """
[
  {
    "id":"dialog-3","coachId":"coach-1","clientUserId":"client-3","peerUserId":"client-3",
    "peerDisplayName":"Глеб","lastMessageSeq":0,"readSeq":0,"peerReadSeq":0,"unreadCount":0,
    "lastMessagePreview":null,"lastMessageAt":null
  }
]
"""

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], application = android.app.Application::class)
class DialogsPagingTest {

    private val backend = MockBackend()

    private val database = TrainerDatabase(
        driver = AndroidSqliteDriver(
            schema = TrainerDatabase.Schema,
            context = ApplicationProvider.getApplicationContext(),
            name = null,
        )
    )

    private val chat = ChatRepositoryImpl(
        httpClientProvider = backend.httpClientProvider,
        localStore = ChatLocalStore(
            database = database,
            mapper = ChatMapper(logger = SilentLogger),
            ioDispatcher = UnconfinedTestDispatcher(),
        ),
        mapper = ChatMapper(logger = SilentLogger),
        presignedUploader = RefusingUploader,
    )

    @Test
    fun `the dialog list asks for a page and stores what came`() = runTest {
        backend.on(
            method = "GET",
            path = "/dialogs",
            status = HttpStatusCode.OK,
            body = DIALOGS_FIRST_PAGE,
            headers = mapOf(NEXT_CURSOR_HEADER to FIRST_CURSOR),
        )

        val refreshed = chat.refreshDialogs()

        assertTrue((refreshed as RequestResult.Success).data, "курсор пришёл — есть что подгружать")
        assertEquals(listOf("limit=$DIALOGS_PAGE_SIZE"), backend.queriesOf("GET", "/dialogs"))
        val stored = chat.observeDialogs().first()
        assertEquals(listOf("dialog-1", "dialog-2"), stored.map { it.id })
        assertEquals("Готова к завтрашней", stored.first().lastMessagePreview)
        assertEquals(2, stored.first().unreadCount)
    }

    @Test
    fun `the next page continues after the cursor and lands under the first one`() = runTest {
        backend.on(
            method = "GET",
            path = "/dialogs",
            status = HttpStatusCode.OK,
            body = DIALOGS_FIRST_PAGE,
            headers = mapOf(NEXT_CURSOR_HEADER to FIRST_CURSOR),
        )
        backend.on(
            method = "GET",
            path = "/dialogs",
            status = HttpStatusCode.OK,
            body = DIALOGS_SECOND_PAGE,
        )

        chat.refreshDialogs()
        val grown = chat.loadMoreDialogs()

        assertFalse((grown as RequestResult.Success).data, "второй страницей список кончился")
        assertEquals(
            listOf("limit=$DIALOGS_PAGE_SIZE", "limit=$DIALOGS_PAGE_SIZE&after=$FIRST_CURSOR"),
            backend.queriesOf("GET", "/dialogs"),
        )
        val stored = chat.observeDialogs().first()
        assertEquals(
            listOf("dialog-1", "dialog-2", "dialog-3"),
            stored.map { it.id },
            "диалог без сообщений стоит после тех, где переписка шла",
        )
    }

    @Test
    fun `without a cursor there is nothing more to ask for`() = runTest {
        backend.on(method = "GET", path = "/dialogs", status = HttpStatusCode.OK, body = DIALOGS_FIRST_PAGE)

        chat.refreshDialogs()
        val more = chat.loadMoreDialogs()

        assertFalse((more as RequestResult.Success).data)
        assertEquals(1, backend.queriesOf("GET", "/dialogs").size, "лишнего запроса не ушло")
    }
}
