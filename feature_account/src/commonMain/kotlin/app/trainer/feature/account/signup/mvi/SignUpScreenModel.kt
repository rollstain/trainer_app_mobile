package app.trainer.feature.account.signup.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.data.auth.FreshSignUp
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.sign_up_email_taken
import app.trainer.strings.sign_up_email_wrong
import app.trainer.strings.sign_up_login_taken
import app.trainer.strings.sign_up_login_wrong
import org.jetbrains.compose.resources.getString

private const val EMAIL_FIELD = "email"
private const val LOGIN_FIELD = "login"
private const val LOGIN_SUGGESTION_SUFFIX = "_1"

class SignUpScreenModel(
    private val authRepository: AuthRepository,
    private val freshSignUp: FreshSignUp,
    private val deviceInfo: String,
) : BaseScreenModel<SignUpState, SignUpSideEffect, SignUpEvent>(
    initialState = SignUpState.initial(),
) {

    override fun onFetchData() = Unit

    override fun dispatch(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.OnNameChanged -> updateState { it.copy(name = event.value) }
            is SignUpEvent.OnEmailChanged -> updateState { it.copy(email = event.value, emailError = null) }
            SignUpEvent.OnLoginRequested -> updateState {
                it.copy(login = LoginField.Shown(value = "", error = null))
            }
            is SignUpEvent.OnLoginChanged -> updateState {
                it.copy(login = LoginField.Shown(value = event.value, error = null))
            }
            SignUpEvent.OnLoginCleared -> updateState { it.copy(login = LoginField.Hidden) }
            is SignUpEvent.OnPasswordChanged -> updateState { it.copy(password = event.value) }
            SignUpEvent.OnRevealToggled -> updateState { it.copy(isPasswordRevealed = !it.isPasswordRevealed) }
            SignUpEvent.OnSubmitClicked -> submit()
        }
    }

    private fun submit() {
        screenModelScope { current ->
            if (!current.isSubmitEnabled) return@screenModelScope
            updateState { it.copy(isSubmitting = true, emailError = null) }
            val signedUp = authRepository.signUpWithPassword(
                displayName = current.name,
                email = current.email,
                login = (current.login as? LoginField.Shown)?.value,
                password = current.password,
                deviceInfo = deviceInfo,
            )
            updateState { it.copy(isSubmitting = false) }
            when (signedUp) {
                is RequestResult.Success -> {
                    freshSignUp.remember(displayName = current.name)
                    postSideEffect(SignUpSideEffect.SignedUp)
                }
                is RequestResult.Error -> showFailure(signedUp)
            }
        }
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        val takenField = failure.fieldErrors.keys.firstOrNull()
        when {
            failure.kind == RequestFailure.Conflict && takenField == LOGIN_FIELD -> showLoginTaken()
            failure.kind == RequestFailure.Conflict && takenField == EMAIL_FIELD -> showEmailTaken()
            failure.kind == RequestFailure.Validation -> showValidation()
            else -> postSideEffect(SignUpSideEffect.ShowFailure(failure))
        }
    }

    private suspend fun showEmailTaken() {
        val message = getString(Res.string.sign_up_email_taken)
        updateState { it.copy(emailError = message) }
    }

    private suspend fun showLoginTaken() {
        val taken = (state.login as? LoginField.Shown)?.value.orEmpty()
        val message = getString(Res.string.sign_up_login_taken, taken + LOGIN_SUGGESTION_SUFFIX)
        updateState { it.copy(login = LoginField.Shown(value = taken, error = message)) }
    }

    private suspend fun showValidation() {
        val typedLogin = (state.login as? LoginField.Shown)?.value
        if (typedLogin != null) {
            val message = getString(Res.string.sign_up_login_wrong)
            updateState { it.copy(login = LoginField.Shown(value = typedLogin, error = message)) }
            return
        }
        val message = getString(Res.string.sign_up_email_wrong)
        updateState { it.copy(emailError = message) }
    }
}
