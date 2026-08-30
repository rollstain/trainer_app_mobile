package app.trainer.data.clients.impl

import app.trainer.data.clients.CoachClient
import app.trainer.data.clients.CoachPolicy
import app.trainer.data.clients.CoachSummary
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.entities.Paged
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.network.HttpClientProvider
import app.trainer.network.safePagedRequest
import app.trainer.network.safeRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ParticipantsRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: ClientsMapper,
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

    override suspend fun coachPolicy(): RequestResult<CoachPolicy> {
        return policyOf(safeRequest { httpClientProvider.client.get("coach/policy") })
    }

    override suspend fun updateCoachPolicy(policy: CoachPolicy): RequestResult<CoachPolicy> {
        val updated = safeRequest<CoachPolicyResponse> {
            httpClientProvider.client.patch("coach/policy") {
                contentType(ContentType.Application.Json)
                setBody(
                    UpdateCoachPolicyRequest(
                        cancellationWindowHours = policy.cancellationWindowHours,
                        reminderHour = policy.reminderHour,
                        sessionRemindersEnabled = policy.sessionRemindersEnabled,
                        diaryRemindersEnabled = policy.diaryRemindersEnabled,
                        checkInRemindersEnabled = policy.checkInRemindersEnabled,
                        workingHours = policy.workingHours.map { day ->
                            WorkingDayWire(
                                dayOfWeek = day.dayOfWeek.name,
                                opensAt = day.opensAt.toString(),
                                closesAt = day.closesAt.toString(),
                            )
                        },
                    )
                )
            }
        }
        return policyOf(updated)
    }

    override suspend fun clientsByIds(userIds: List<String>): RequestResult<List<CoachClient>> {
        if (userIds.isEmpty()) return RequestResult.Success(emptyList())
        val loaded = safeRequest<List<CoachClientResponse>> {
            httpClientProvider.client.get("coach/clients") {
                userIds.forEach { parameter("ids", it) }
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toCoachClient))
        }
    }

    private fun policyOf(response: RequestResult<CoachPolicyResponse>): RequestResult<CoachPolicy> {
        return when (response) {
            is RequestResult.Error -> response
            is RequestResult.Success -> {
                val policy = mapper.toCoachPolicy(response.data)
                if (policy == null) {
                    RequestResult.Error(
                        kind = RequestFailure.Parsing,
                        statusCode = null,
                        userMessage = "Не удалось прочитать настройки тренера",
                        devMessage = "CoachPolicyResponse без обязательных полей",
                    )
                } else {
                    RequestResult.Success(policy)
                }
            }
        }
    }

    override suspend fun clientsOfCoach(
        limit: Int?,
        after: String?,
        query: String?,
    ): RequestResult<Paged<List<CoachClient>>> {
        val loaded = safePagedRequest<List<CoachClientResponse>> {
            httpClientProvider.client.get("coach/clients") {
                limit?.let { parameter("limit", it) }
                after?.let { parameter("after", it) }
                query?.let { parameter("query", it) }
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(
                Paged(
                    items = loaded.data.items.mapNotNull(mapper::toCoachClient),
                    nextCursor = loaded.data.nextCursor,
                )
            )
        }
    }

    override suspend fun missedSessions(clientUserIds: List<String>): RequestResult<Map<String, Int>> {
        if (clientUserIds.isEmpty()) return RequestResult.Success(emptyMap())
        val loaded = safeRequest<List<MissedSessionsResponse>> {
            httpClientProvider.client.get("coach/clients/missed-sessions") {
                clientUserIds.forEach { parameter("clientUserIds", it) }
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(
                loaded.data.mapNotNull { row ->
                    val clientUserId = row.clientUserId ?: return@mapNotNull null
                    val missed = row.missedInARow ?: return@mapNotNull null
                    clientUserId to missed
                }.toMap()
            )
        }
    }

    override suspend fun archiveClient(clientUserId: String): RequestResult<Unit> {
        return safeRequest { httpClientProvider.client.delete("coach/clients/$clientUserId") }
    }
}
