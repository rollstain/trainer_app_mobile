package app.trainer.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

enum class DiaryPeriod { Week, Month, Range }

sealed interface Screens : NavKey {

    @Serializable
    data class Welcome(val afterSessionExpiry: Boolean) : Screens

    @Serializable
    data class Invite(val afterSessionExpiry: Boolean) : Screens

    @Serializable
    data class InviteLink(val code: String) : Screens

    @Serializable
    data class Onboarding(val code: String) : Screens

    @Serializable
    data object NoCoach : Screens

    @Serializable
    data object CoachSetup : Screens

    @Serializable
    data object ContactLink : Screens

    @Serializable
    data object CoachToday : Screens

    @Serializable
    data object ClientNext : Screens

    @Serializable
    data object CoachChats : Screens

    @Serializable
    data class Chat(val dialogId: String) : Screens

    @Serializable
    data class CoachCalendar(val weekStartIso: String?) : Screens

    @Serializable
    data class NewSlot(val dateIso: String?) : Screens

    @Serializable
    data class SlotSeriesResult(val batchId: String) : Screens

    @Serializable
    data object CoachDiaries : Screens

    @Serializable
    data class CoachClientDiary(val clientUserId: String, val period: DiaryPeriod) : Screens

    @Serializable
    data object CoachPeople : Screens

    @Serializable
    data class ClientCard(val clientUserId: String) : Screens

    @Serializable
    data class GroupSession(val slotId: String) : Screens

    @Serializable
    data object NewExercise : Screens

    @Serializable
    data object ExerciseLibrary : Screens

    @Serializable
    data object Programs : Screens

    @Serializable
    data class ProgramEditor(val programId: String) : Screens

    @Serializable
    data class ProgramDay(
        val programId: String,
        val weekNumber: Int,
        val dayOfWeek: Int,
    ) : Screens

    @Serializable
    data class ClientBooking(val coachId: String?, val weekStartIso: String?) : Screens

    @Serializable
    data class ClientDiaryDay(val dateIso: String) : Screens

    @Serializable
    data object Progress : Screens

    @Serializable
    data class CheckIn(val dateIso: String) : Screens

    @Serializable
    data class PhotoCompare(val clientUserId: String?) : Screens

    @Serializable
    data object FormChecks : Screens

    @Serializable
    data object CoachFormChecks : Screens

    @Serializable
    data object Profile : Screens

    @Serializable
    data object Devices : Screens

    @Serializable
    data object LoginMethods : Screens
}
