package app.trainer.data.progress.impl

import app.trainer.data.progress.Habit
import app.trainer.data.progress.HabitsRepository
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.safeRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate

class HabitsRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: ProgressMapper,
) : HabitsRepository {

    private val client get() = httpClientProvider.client

    override suspend fun ownHabits(from: LocalDate, to: LocalDate): RequestResult<List<Habit>> {
        return habitsOf {
            client.get("habits") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
    }

    override suspend fun clientHabits(
        clientUserId: String,
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<Habit>> {
        return habitsOf {
            client.get("coach/clients/$clientUserId/habits") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
    }

    override suspend fun createOwn(title: String): RequestResult<Habit> {
        return habitOf {
            client.post("habits") {
                contentType(ContentType.Application.Json)
                setBody(CreateHabitRequest(title = title))
            }
        }
    }

    override suspend fun createForClient(clientUserId: String, title: String): RequestResult<Habit> {
        return habitOf {
            client.post("coach/clients/$clientUserId/habits") {
                contentType(ContentType.Application.Json)
                setBody(CreateHabitRequest(title = title))
            }
        }
    }

    override suspend fun archive(habitId: String): RequestResult<Unit> {
        return safeRequest { client.delete("habits/$habitId") }
    }

    override suspend fun mark(habitId: String, date: LocalDate, isDone: Boolean): RequestResult<Unit> {
        return safeRequest {
            if (isDone) {
                client.post("habits/$habitId/marks/$date")
            } else {
                client.delete("habits/$habitId/marks/$date")
            }
        }
    }

    private suspend fun habitsOf(request: suspend () -> HttpResponse): RequestResult<List<Habit>> {
        val loaded = safeRequest<List<HabitResponse>> { request() }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toHabit))
        }
    }

    private suspend fun habitOf(request: suspend () -> HttpResponse): RequestResult<Habit> {
        val loaded = safeRequest<HabitResponse> { request() }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                val habit = mapper.toHabit(loaded.data)
                if (habit == null) {
                    RequestResult.Error(
                        statusCode = null,
                        userMessage = "Не удалось прочитать привычку",
                        devMessage = "HabitResponse не разобран",
                    )
                } else {
                    RequestResult.Success(habit)
                }
            }
        }
    }
}
