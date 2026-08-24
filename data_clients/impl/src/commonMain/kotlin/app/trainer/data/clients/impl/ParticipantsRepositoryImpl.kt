package app.trainer.data.clients.impl

import app.trainer.data.clients.CoachClient
import app.trainer.data.clients.CoachSummary
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.safeRequest
import app.trainer.logger.Logger
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val LOG_TAG = "participants-repository"

class ParticipantsRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: ClientsMapper,
    private val logger: Logger,
) : ParticipantsRepository {

    override suspend fun coachesOfClient(): RequestResult<List<CoachSummary>> {
        val loaded = safeRequest<List<CoachSummaryResponse>> {
            httpClientProvider.client.get("me/coaches")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toCoachSummary))
        }
    }

    override suspend fun coachPolicy(): RequestResult<Int> {
        return windowHoursOf(safeRequest { httpClientProvider.client.get("coach/policy") })
    }

    override suspend fun updateCoachPolicy(cancellationWindowHours: Int): RequestResult<Int> {
        val updated = safeRequest<CoachPolicyResponse> {
            httpClientProvider.client.patch("coach/policy") {
                contentType(ContentType.Application.Json)
                setBody(UpdateCoachPolicyRequest(cancellationWindowHours = cancellationWindowHours))
            }
        }
        return windowHoursOf(updated)
    }

    private fun windowHoursOf(response: RequestResult<CoachPolicyResponse>): RequestResult<Int> {
        return when (response) {
            is RequestResult.Error -> response
            is RequestResult.Success -> {
                val hours = response.data.cancellationWindowHours
                if (hours == null) {
                    logger.error(tag = LOG_TAG, message = "В ответе нет cancellationWindowHours")
                    RequestResult.Error(
                        statusCode = null,
                        userMessage = "Не удалось прочитать настройку отмены",
                        devMessage = "CoachPolicyResponse без cancellationWindowHours",
                    )
                } else {
                    RequestResult.Success(hours)
                }
            }
        }
    }

    override suspend fun clientsOfCoach(): RequestResult<List<CoachClient>> {
        val loaded = safeRequest<List<CoachClientResponse>> {
            httpClientProvider.client.get("coach/clients")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toCoachClient))
        }
    }
}
