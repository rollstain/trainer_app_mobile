package app.trainer.android.paging

import app.trainer.data.progress.AwaitingCheckIn
import app.trainer.data.progress.CheckIn
import app.trainer.data.progress.CheckInDraft
import app.trainer.data.progress.CheckInRepository
import app.trainer.data.progress.PreparedPhotoUpload
import app.trainer.entities.Paged
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.feature.progress.presentation.checkin.mvi.CoachCheckInsEvent
import app.trainer.feature.progress.presentation.checkin.mvi.CoachCheckInsScreenModel
import app.trainer.logger.ConsoleLogger
import app.trainer.logger.Logger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

private const val SECOND_PAGE_CURSOR = "cursor-page-2"

@OptIn(ExperimentalCoroutinesApi::class)
class QueueScreenFlowTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun start() {
        Dispatchers.setMain(dispatcher)
        startKoin { modules(module { single<Logger> { ConsoleLogger() } }) }
    }

    @After
    fun stop() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `reaching the end of the queue asks for the next page once`() = runTest {
        val repository = PagedCheckIns(
            pages = listOf(
                Paged(items = awaiting("check-in-1", "check-in-2"), nextCursor = SECOND_PAGE_CURSOR),
                Paged(items = awaiting("check-in-3"), nextCursor = null),
            )
        )
        val model = CoachCheckInsScreenModel(checkInRepository = repository)

        model.dispatch(CoachCheckInsEvent.OnEndReached)
        model.dispatch(CoachCheckInsEvent.OnEndReached)

        assertEquals(
            listOf("check-in-1", "check-in-2", "check-in-3"),
            model.stateChanges.value.checkIns.map { it.checkInId },
        )
        assertFalse(model.stateChanges.value.hasMore, "вторая страница пришла без курсора — очередь кончилась")
        assertEquals(listOf(null, SECOND_PAGE_CURSOR), repository.requestedCursors)
    }

    @Test
    fun `a failed page keeps what is already shown and lets the user try again`() = runTest {
        val repository = PagedCheckIns(
            pages = listOf(Paged(items = awaiting("check-in-1"), nextCursor = SECOND_PAGE_CURSOR)),
            failAfterPages = 1,
        )
        val model = CoachCheckInsScreenModel(checkInRepository = repository)

        model.dispatch(CoachCheckInsEvent.OnEndReached)

        assertEquals(listOf("check-in-1"), model.stateChanges.value.checkIns.map { it.checkInId })
        assertFalse(model.stateChanges.value.isLoadingMore, "подгрузка закончилась отказом, а не зависла")
        assertTrue(model.stateChanges.value.hasMore, "курсор остался — можно повторить")
    }

    private fun awaiting(vararg ids: String): List<AwaitingCheckIn> = ids.map { id ->
        AwaitingCheckIn(
            checkInId = id,
            clientUserId = "client-$id",
            clientDisplayName = "Анна",
            checkInDate = LocalDate(2026, 3, 1),
        )
    }
}

private class PagedCheckIns(
    private val pages: List<Paged<List<AwaitingCheckIn>>>,
    private val failAfterPages: Int = Int.MAX_VALUE,
) : CheckInRepository {

    val requestedCursors = mutableListOf<String?>()

    override suspend fun awaitingReview(
        limit: Int,
        after: String?,
    ): RequestResult<Paged<List<AwaitingCheckIn>>> {
        requestedCursors += after
        val page = pages.getOrNull(requestedCursors.size - 1)
        if (page == null || requestedCursors.size > failAfterPages) {
            return RequestResult.Error(
                kind = RequestFailure.Network,
                statusCode = null,
                userMessage = "Сеть недоступна",
                devMessage = "тестовый отказ",
            )
        }
        return RequestResult.Success(page)
    }

    override suspend fun ownCheckIns(from: LocalDate, to: LocalDate): RequestResult<List<CheckIn>> =
        error("не участвует в проверке очереди")

    override suspend fun clientCheckIns(
        clientUserId: String,
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<CheckIn>> = error("не участвует в проверке очереди")

    override suspend fun save(checkInDate: LocalDate, draft: CheckInDraft): RequestResult<CheckIn> =
        error("не участвует в проверке очереди")

    override suspend fun review(
        clientUserId: String,
        checkInId: String,
        comment: String?,
    ): RequestResult<CheckIn> = error("не участвует в проверке очереди")

    override suspend fun preparePhotoUpload(
        fileName: String,
        contentType: String,
        sizeBytes: Long,
    ): RequestResult<PreparedPhotoUpload> = error("не участвует в проверке очереди")

    override suspend fun uploadPhoto(
        uploadUrl: String,
        contentType: String,
        bytes: ByteArray,
    ): RequestResult<Unit> = error("не участвует в проверке очереди")

    override suspend fun deletePhoto(photoId: String): RequestResult<Unit> =
        error("не участвует в проверке очереди")
}
