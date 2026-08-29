package app.trainer.feature.account.passwordform.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.password_form_current_wrong
import app.trainer.strings.sign_up_email_taken
import org.jetbrains.compose.resources.getString

private const val EMAIL_FIELD = "email"

class PasswordFormScreenModel(
    private val identitiesRepository: IdentitiesRepository,
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<PasswordFormState, PasswordFormSideEffect, PasswordFormEvent>(
    initialState = PasswordFormState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            val profile = profileRepository.me()
            if (profile !is RequestResult.Success) {
                updateState { it.copy(isLoading = false) }
                return@onFetchDataScope
            }
            updateState {
                it.copy(
                    isLoading = false,
                    hasPassword = profile.data.hasPassword,
                    needsEmail = profile.data.email == null,
                    email = profile.data.email.orEmpty(),
                )
            }
        }
    }

    override fun dispatch(event: PasswordFormEvent) {
        when (event) {
            is PasswordFormEvent.OnEmailChanged -> updateState {
                it.copy(email = event.value, emailError = null)
            }
            is PasswordFormEvent.OnCurrentPasswordChanged -> updateState {
                it.copy(currentPassword = event.value, currentPasswordError = null)
            }
            is PasswordFormEvent.OnNewPasswordChanged -> updateState { it.copy(newPassword = event.value) }
            PasswordFormEvent.OnRevealToggled -> updateState { it.copy(isRevealed = !it.isRevealed) }
            PasswordFormEvent.OnSaveClicked -> save()
            PasswordFormEvent.OnRecoveryClicked -> screenModelScope { current ->
                postSideEffect(PasswordFormSideEffect.OpenRecovery(email = current.email))
            }
        }
    }

    private fun save() {
        screenModelScope { current ->
            if (!current.isSaveEnabled) return@screenModelScope
            updateState { it.copy(isSaving = true, currentPasswordError = null, emailError = null) }
            val saved = identitiesRepository.setPassword(
                email = current.email.takeIf { current.needsEmail },
                login = null,
                currentPassword = current.currentPassword.takeIf { current.hasPassword },
                newPassword = current.newPassword,
            )
            updateState { it.copy(isSaving = false) }
            when (saved) {
                is RequestResult.Success -> postSideEffect(PasswordFormSideEffect.Saved)
                is RequestResult.Error -> showFailure(saved)
            }
        }
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        when {
            failure.kind == RequestFailure.Forbidden -> {
                val message = getString(Res.string.password_form_current_wrong)
                updateState { it.copy(currentPasswordError = message) }
            }
            failure.kind == RequestFailure.Conflict && failure.fieldErrors.containsKey(EMAIL_FIELD) -> {
                val message = getString(Res.string.sign_up_email_taken)
                updateState { it.copy(emailError = message) }
            }
            else -> postSideEffect(PasswordFormSideEffect.ShowFailure(failure))
        }
    }
}
