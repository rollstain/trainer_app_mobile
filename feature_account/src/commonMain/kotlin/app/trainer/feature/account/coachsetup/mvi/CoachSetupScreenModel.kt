package app.trainer.feature.account.coachsetup.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestResult
import kotlinx.datetime.TimeZone

class CoachSetupScreenModel(
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<CoachSetupState, CoachSetupSideEffect, CoachSetupEvent>(
    initialState = CoachSetupState.initial(zoneId = TimeZone.currentSystemDefault().id),
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
        }
    }

    override fun dispatch(event: CoachSetupEvent) {
        when (event) {
            is CoachSetupEvent.OnDisplayNameChanged -> updateState {
                it.copy(displayName = event.displayName)
            }
            CoachSetupEvent.OnStartClicked -> start()
        }
    }

    private fun start() {
        screenModelScope { current ->
            if (!current.isStartEnabled) return@screenModelScope
            updateState { it.copy(isSending = true) }
            val created = profileRepository.becomeCoach(
                displayName = current.displayName,
                zoneId = current.zoneId,
            )
            updateState { it.copy(isSending = false) }
            when (created) {
                is RequestResult.Error -> postSideEffect(CoachSetupSideEffect.ShowFailure(created))
                is RequestResult.Success -> postSideEffect(CoachSetupSideEffect.Started)
            }
        }
    }
}
