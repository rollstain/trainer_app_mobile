package app.trainer.feature.account.application.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestResult

class ApplicationScreenModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<ApplicationState, ApplicationSideEffect, ApplicationEvent>(
    initialState = ApplicationState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            val profile = profileRepository.me()
            val access = authRepository.coachAccess()
            updateState { current ->
                current.copy(
                    displayName = (profile as? RequestResult.Success)?.data?.displayName.orEmpty(),
                    about = (access as? RequestResult.Success)?.data?.about.orEmpty(),
                    isLoading = false,
                )
            }
        }
    }

    override fun dispatch(event: ApplicationEvent) {
        when (event) {
            is ApplicationEvent.OnDisplayNameChanged -> updateState {
                it.copy(displayName = event.displayName)
            }
            is ApplicationEvent.OnAboutChanged -> updateState {
                it.copy(about = event.about.take(ABOUT_MAX_LENGTH), isTooShortShown = false)
            }
            ApplicationEvent.OnSendClicked -> send()
        }
    }

    private fun send() {
        screenModelScope { current ->
            if (current.isSending) return@screenModelScope
            if (!current.isSendEnabled) {
                updateState { it.copy(isTooShortShown = true) }
                return@screenModelScope
            }
            updateState { it.copy(isSending = true) }
            val sent = authRepository.askCoachAccess(
                displayName = current.displayName,
                about = current.about,
            )
            updateState { it.copy(isSending = false) }
            when (sent) {
                is RequestResult.Error -> postSideEffect(ApplicationSideEffect.ShowFailure(sent))
                is RequestResult.Success -> postSideEffect(ApplicationSideEffect.Sent)
            }
        }
    }
}
