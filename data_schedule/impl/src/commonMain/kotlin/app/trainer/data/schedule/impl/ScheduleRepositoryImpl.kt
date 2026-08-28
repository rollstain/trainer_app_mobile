package app.trainer.data.schedule.impl

import app.trainer.data.schedule.ClientSchedule
import app.trainer.data.schedule.ClientSlot
import app.trainer.data.schedule.CoachSchedule
import app.trainer.data.schedule.CoachSlot
import app.trainer.data.schedule.ScheduleRepository
import app.trainer.data.schedule.SlotChangeKind
import app.trainer.data.schedule.SlotChangeRequest
import app.trainer.data.schedule.SlotSeriesDraft
import app.trainer.data.schedule.SlotSeriesResult
import app.trainer.entities.RequestFailure
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
import kotlin.time.Instant

class ScheduleRepositoryImpl(
    private val httpClientProvider: HttpClientProvider,
    private val mapper: ScheduleMapper,
) : ScheduleRepository {

    private val client get() = httpClientProvider.client

    override suspend fun coachSchedule(from: Instant, to: Instant): RequestResult<CoachSchedule> {
        val loaded = safeRequest<CoachScheduleResponse> {
            client.get("schedule/coach") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                val schedule = mapper.toCoachSchedule(loaded.data)
                if (schedule == null) mappingFailed("CoachSchedule") else RequestResult.Success(schedule)
            }
        }
    }

    override suspend fun clientSchedule(
        coachId: String,
        from: Instant,
        to: Instant,
    ): RequestResult<ClientSchedule> {
        val loaded = safeRequest<ClientScheduleResponse> {
            client.get("schedule/coaches/$coachId") {
                parameter("from", from.toString())
                parameter("to", to.toString())
            }
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> {
                val schedule = mapper.toClientSchedule(loaded.data)
                if (schedule == null) mappingFailed("ClientSchedule") else RequestResult.Success(schedule)
            }
        }
    }

    override suspend fun createSlot(
        startsAt: Instant,
        durationMinutes: Int,
        capacity: Int,
    ): RequestResult<CoachSlot> {
        val created = safeRequest<CoachSlotResponse> {
            client.post("schedule/slots") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateSlotRequest(
                        startsAt = startsAt.toString(),
                        durationMinutes = durationMinutes,
                        capacity = capacity,
                    )
                )
            }
        }
        return coachSlotOrError(created)
    }

    override suspend fun createSlotSeries(draft: SlotSeriesDraft): RequestResult<SlotSeriesResult> {
        val created = safeRequest<CreateSlotSeriesResponse> {
            client.post("schedule/slots/series") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateSlotSeriesRequest(
                        startDate = draft.startDate.toString(),
                        weeksCount = draft.weeksCount,
                        daysOfWeek = draft.daysOfWeek.map { it.name },
                        timeOfDay = draft.timeOfDay.toString(),
                        durationMinutes = draft.durationMinutes,
                        capacity = draft.capacity,
                    )
                )
            }
        }
        return when (created) {
            is RequestResult.Error -> created
            is RequestResult.Success -> RequestResult.Success(mapper.toSeriesResult(created.data))
        }
    }

    override suspend fun assignSlot(slotId: String, clientUserId: String): RequestResult<CoachSlot> {
        val assigned = safeRequest<CoachSlotResponse> {
            client.post("schedule/slots/$slotId/assign") {
                contentType(ContentType.Application.Json)
                setBody(AssignSlotRequest(clientUserId = clientUserId))
            }
        }
        return coachSlotOrError(assigned)
    }

    override suspend fun removeParticipant(slotId: String, clientUserId: String): RequestResult<CoachSlot> {
        val removed = safeRequest<CoachSlotResponse> {
            client.delete("schedule/slots/$slotId/participants/$clientUserId")
        }
        return coachSlotOrError(removed)
    }

    override suspend fun cancelSlot(slotId: String): RequestResult<CoachSlot> {
        val cancelled = safeRequest<CoachSlotResponse> {
            client.post("schedule/slots/$slotId/cancel")
        }
        return coachSlotOrError(cancelled)
    }

    override suspend fun completeSlot(slotId: String): RequestResult<CoachSlot> {
        val completed = safeRequest<CoachSlotResponse> {
            client.post("schedule/slots/$slotId/complete")
        }
        return coachSlotOrError(completed)
    }

    override suspend fun bookSlot(slotId: String): RequestResult<ClientSlot> {
        return clientSlotOf { client.post("schedule/slots/$slotId/book") }
    }

    override suspend fun joinWaitlist(slotId: String): RequestResult<ClientSlot> {
        return clientSlotOf { client.post("schedule/slots/$slotId/waitlist") }
    }

    override suspend fun leaveWaitlist(slotId: String): RequestResult<ClientSlot> {
        return clientSlotOf { client.delete("schedule/slots/$slotId/waitlist") }
    }

    private suspend fun clientSlotOf(request: suspend () -> HttpResponse): RequestResult<ClientSlot> {
        val response = safeRequest<ClientSlotResponse> { request() }
        return when (response) {
            is RequestResult.Error -> response
            is RequestResult.Success -> {
                val slot = mapper.toClientSlot(response.data)
                if (slot == null) mappingFailed("ClientSlot") else RequestResult.Success(slot)
            }
        }
    }

    override suspend fun requestChange(
        slotId: String,
        kind: SlotChangeKind,
        proposedStartsAt: Instant?,
    ): RequestResult<SlotChangeRequest> {
        val requested = safeRequest<SlotChangeRequestResponse> {
            client.post("schedule/slots/$slotId/change-requests") {
                contentType(ContentType.Application.Json)
                setBody(
                    SlotChangeRequestBody(
                        kind = kind.name,
                        proposedStartsAt = proposedStartsAt?.toString(),
                    )
                )
            }
        }
        return changeRequestOrError(requested)
    }

    override suspend fun pendingChangeRequests(): RequestResult<List<SlotChangeRequest>> {
        val loaded = safeRequest<List<SlotChangeRequestResponse>> {
            client.get("schedule/change-requests/pending")
        }
        return when (loaded) {
            is RequestResult.Error -> loaded
            is RequestResult.Success -> RequestResult.Success(loaded.data.mapNotNull(mapper::toChangeRequest))
        }
    }

    override suspend fun resolveChangeRequest(
        requestId: String,
        approve: Boolean,
    ): RequestResult<SlotChangeRequest> {
        val resolved = safeRequest<SlotChangeRequestResponse> {
            client.post("schedule/change-requests/$requestId/resolve") {
                contentType(ContentType.Application.Json)
                setBody(ResolveChangeRequestBody(approve = approve))
            }
        }
        return changeRequestOrError(resolved)
    }

    private fun coachSlotOrError(result: RequestResult<CoachSlotResponse>): RequestResult<CoachSlot> {
        return when (result) {
            is RequestResult.Error -> result
            is RequestResult.Success -> {
                val slot = mapper.toCoachSlot(result.data)
                if (slot == null) mappingFailed("CoachSlot") else RequestResult.Success(slot)
            }
        }
    }

    private fun changeRequestOrError(
        result: RequestResult<SlotChangeRequestResponse>,
    ): RequestResult<SlotChangeRequest> {
        return when (result) {
            is RequestResult.Error -> result
            is RequestResult.Success -> {
                val request = mapper.toChangeRequest(result.data)
                if (request == null) mappingFailed("SlotChangeRequest") else RequestResult.Success(request)
            }
        }
    }

    private fun mappingFailed(entity: String): RequestResult.Error = RequestResult.Error(
        kind = RequestFailure.Parsing,
        statusCode = null,
        userMessage = "",
        devMessage = "Ответ сервера не удалось разобрать в $entity",
    )
}
