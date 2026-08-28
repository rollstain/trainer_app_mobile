package app.trainer.feature.account.onboarding.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.onboarding_name_empty_message
import app.trainer.strings.onboarding_name_too_long_message
import org.jetbrains.compose.resources.getString

const val DISPLAY_NAME_MAX_LENGTH = 40

class OnboardingScreenModel(
    private val code: String,
    private val authRepository: AuthRepository,
    private val deviceInfo: String,
) : BaseScreenModel<OnboardingState, OnboardingSideEffect, OnboardingEvent>(
    initialState = OnboardingState.initial(),
) {

    override fun onFetchData() = Unit

    override fun dispatch(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.OnDisplayNameChanged -> updateState {
                it.copy(displayName = event.displayName, nameError = null)
            }
            OnboardingEvent.OnContinueClicked -> join()
        }
    }

    private fun join() {
        screenModelScope { current ->
            if (current.isSaving) return@screenModelScope
            val name = current.displayName.trim()
            val nameError = when {
                name.isEmpty() -> getString(Res.string.onboarding_name_empty_message)
                name.length > DISPLAY_NAME_MAX_LENGTH -> getString(Res.string.onboarding_name_too_long_message)
                else -> null
            }
            if (nameError != null) {
                updateState { it.copy(nameError = nameError) }
                return@screenModelScope
            }
            updateState { it.copy(isSaving = true) }
            val joined = authRepository.redeemInvite(
                code = code,
                displayName = name,
                deviceInfo = deviceInfo,
            )
            updateState { it.copy(isSaving = false) }
            when (joined) {
                is RequestResult.Error -> postSideEffect(OnboardingSideEffect.ShowFailure(joined))
                is RequestResult.Success -> postSideEffect(OnboardingSideEffect.SignedIn)
            }
        }
    }
}
