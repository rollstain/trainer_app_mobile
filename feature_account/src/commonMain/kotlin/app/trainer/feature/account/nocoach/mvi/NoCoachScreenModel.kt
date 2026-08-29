package app.trainer.feature.account.nocoach.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.base.date.dayMonthOf
import app.trainer.base.date.monthGenitiveOf
import app.trainer.base.date.timeOfDayOf
import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.CoachAccess
import app.trainer.data.auth.CoachAccessStatus
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.invite_code_expired_message
import app.trainer.strings.invite_code_not_found_message
import app.trainer.strings.invite_code_used_message
import app.trainer.strings.no_coach_asked_at
import app.trainer.uikit.widgets.CODE_LENGTH
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString

class NoCoachScreenModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<NoCoachState, NoCoachSideEffect, NoCoachEvent>(
    initialState = NoCoachState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            val profile = profileRepository.me()
            if (profile is RequestResult.Success) {
                updateState { it.copy(displayName = profile.data.displayName) }
            }
            val access = authRepository.coachAccess()
            if (access is RequestResult.Success) {
                val shown = accessOf(access.data)
                updateState { it.copy(access = shown) }
            }
        }
    }

    override fun dispatch(event: NoCoachEvent) {
        when (event) {
            NoCoachEvent.OnReloadRequested -> onFetchData()
            is NoCoachEvent.OnCodeChanged -> changeCode(event.code)
            NoCoachEvent.OnJoinClicked -> join()
            NoCoachEvent.OnApplicationClicked -> screenModelScope {
                postSideEffect(NoCoachSideEffect.OpenApplication)
            }
            NoCoachEvent.OnSignOutClicked -> updateState { it.copy(isSignOutDialogVisible = true) }
            NoCoachEvent.OnSignOutDismissed -> updateState { it.copy(isSignOutDialogVisible = false) }
            NoCoachEvent.OnSignOutConfirmed -> signOut()
        }
    }

    private fun changeCode(code: String) {
        val isPasted = code.length - state.code.length > 1
        updateState { it.copy(code = code, codeError = null) }
        if (isPasted && code.length == CODE_LENGTH) {
            join()
        }
    }

    private fun join() {
        screenModelScope { current ->
            if (!current.isSubmitEnabled) return@screenModelScope
            updateState { it.copy(isJoining = true, codeError = null) }
            val joined = authRepository.joinCoach(current.code)
            updateState { it.copy(isJoining = false) }
            when (joined) {
                is RequestResult.Error -> showCodeError(joined)
                is RequestResult.Success -> postSideEffect(NoCoachSideEffect.Joined)
            }
        }
    }

    private fun signOut() {
        screenModelScope {
            updateState { it.copy(isSignOutDialogVisible = false) }
            authRepository.logout()
        }
    }

    private suspend fun accessOf(access: CoachAccess): CoachAccessState = when (access.status) {
        CoachAccessStatus.PENDING -> CoachAccessState.Pending(
            askedAtLabel = askedAtLabelOf(access.askedAtIso),
        )
        CoachAccessStatus.DECLINED -> declinedOf(access)
        CoachAccessStatus.NONE, CoachAccessStatus.APPROVED -> CoachAccessState.NotAsked
    }

    private suspend fun askedAtLabelOf(askedAtIso: String?): String {
        if (askedAtIso == null) return ""
        val askedAt = Instant.parse(askedAtIso).toLocalDateTime(TimeZone.currentSystemDefault())
        return getString(Res.string.no_coach_asked_at, dayMonthOf(askedAt.date), timeOfDayOf(askedAt))
    }

    private suspend fun declinedOf(access: CoachAccess): CoachAccessState.Declined {
        val decidedAt = access.decidedAtIso
            ?.let { Instant.parse(it).toLocalDateTime(TimeZone.currentSystemDefault()).date }
        val canAskAgainOn = access.canAskAgainOn?.let(LocalDate::parse)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return CoachAccessState.Declined(
            decidedAtLabel = decidedAt?.let { dayMonthOf(it) }.orEmpty(),
            canAskAgainLabel = canAskAgainOn?.let { "${it.day} ${monthGenitiveOf(it)}" }.orEmpty(),
            canAskAgain = canAskAgainOn == null || canAskAgainOn <= today,
        )
    }

    private suspend fun showCodeError(failure: RequestResult.Error) {
        val message = when (failure.kind) {
            RequestFailure.Gone -> getString(Res.string.invite_code_expired_message)
            RequestFailure.Conflict -> getString(Res.string.invite_code_used_message)
            RequestFailure.NotFound -> getString(Res.string.invite_code_not_found_message)
            else -> null
        }
        if (message == null) {
            postSideEffect(NoCoachSideEffect.ShowFailure(failure))
            return
        }
        val keepsTypedCode = failure.kind == RequestFailure.NotFound
        updateState { current ->
            current.copy(
                codeError = message,
                code = if (keepsTypedCode) current.code else "",
            )
        }
    }
}
