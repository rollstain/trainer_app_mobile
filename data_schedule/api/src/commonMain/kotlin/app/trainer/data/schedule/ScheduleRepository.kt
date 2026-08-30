package app.trainer.data.schedule

import app.trainer.entities.Paged
import app.trainer.entities.RequestResult
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class SlotSeriesDraft(
    val startDate: LocalDate,
    val weeksCount: Int,
    val daysOfWeek: Set<DayOfWeek>,
    val timeOfDay: LocalTime,
    val durationMinutes: Int,
    val capacity: Int,
)

interface CoachScheduleRepository {

    suspend fun coachSchedule(from: Instant, to: Instant): RequestResult<CoachSchedule>

    suspend fun coachSlot(slotId: String): RequestResult<CoachSlot>

    suspend fun createSlot(startsAt: Instant, durationMinutes: Int, capacity: Int): RequestResult<CoachSlot>

    suspend fun createSlotSeries(draft: SlotSeriesDraft): RequestResult<SlotSeriesResult>

    suspend fun assignSlot(slotId: String, clientUserId: String): RequestResult<CoachSlot>

    suspend fun removeParticipant(slotId: String, clientUserId: String): RequestResult<CoachSlot>

    suspend fun cancelSlot(slotId: String): RequestResult<CoachSlot>

    suspend fun completeSlot(slotId: String): RequestResult<CoachSlot>

    suspend fun pendingChangeRequests(limit: Int, after: String?): RequestResult<Paged<List<SlotChangeRequest>>>

    suspend fun pendingChangeRequestsBetween(from: Instant, to: Instant): RequestResult<List<SlotChangeRequest>>

    suspend fun resolveChangeRequest(requestId: String, approve: Boolean): RequestResult<SlotChangeRequest>
}

interface ClientScheduleRepository {

    suspend fun clientSchedule(coachId: String, from: Instant, to: Instant): RequestResult<ClientSchedule>

    suspend fun bookSlot(slotId: String): RequestResult<ClientSlot>

    suspend fun joinWaitlist(slotId: String): RequestResult<ClientSlot>

    suspend fun leaveWaitlist(slotId: String): RequestResult<ClientSlot>

    suspend fun requestChange(
        slotId: String,
        kind: SlotChangeKind,
        proposedStartsAt: Instant?,
    ): RequestResult<SlotChangeRequest>
}
