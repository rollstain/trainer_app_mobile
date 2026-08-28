package app.trainer.data.progress

import app.trainer.entities.RequestResult
import kotlinx.datetime.LocalDate

interface CheckInRepository {

    suspend fun ownCheckIns(from: LocalDate, to: LocalDate): RequestResult<List<CheckIn>>

    suspend fun clientCheckIns(
        clientUserId: String,
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<CheckIn>>

    suspend fun save(checkInDate: LocalDate, draft: CheckInDraft): RequestResult<CheckIn>

    suspend fun awaitingReview(): RequestResult<List<AwaitingCheckIn>>

    suspend fun review(
        clientUserId: String,
        checkInId: String,
        comment: String?,
    ): RequestResult<CheckIn>

    suspend fun preparePhotoUpload(
        fileName: String,
        contentType: String,
        sizeBytes: Long,
    ): RequestResult<PreparedPhotoUpload>

    suspend fun uploadPhoto(
        uploadUrl: String,
        contentType: String,
        bytes: ByteArray,
    ): RequestResult<Unit>

    suspend fun deletePhoto(photoId: String): RequestResult<Unit>
}

interface HabitsRepository {

    suspend fun ownHabits(from: LocalDate, to: LocalDate): RequestResult<List<Habit>>

    suspend fun clientHabits(
        clientUserId: String,
        from: LocalDate,
        to: LocalDate,
    ): RequestResult<List<Habit>>

    suspend fun createOwn(title: String): RequestResult<Habit>

    suspend fun createForClient(clientUserId: String, title: String): RequestResult<Habit>

    suspend fun archive(habitId: String): RequestResult<Unit>

    suspend fun mark(habitId: String, date: LocalDate, isDone: Boolean): RequestResult<Unit>
}

interface FormCheckRepository {

    suspend fun ownFormChecks(): RequestResult<List<FormCheck>>

    suspend fun awaitingReview(): RequestResult<List<FormCheck>>

    suspend fun submit(
        fileName: String,
        contentType: String,
        bytes: ByteArray,
        exerciseId: String?,
        note: String?,
    ): RequestResult<FormCheck>

    suspend fun review(formCheckId: String, comment: String?): RequestResult<FormCheck>
}
