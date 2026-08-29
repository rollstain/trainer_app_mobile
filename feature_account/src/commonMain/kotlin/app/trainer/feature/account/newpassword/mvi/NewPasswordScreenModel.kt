package app.trainer.feature.account.newpassword.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult

class NewPasswordScreenModel(
    private val resetToken: String?,
    private val claimToken: String?,
    private val authRepository: AuthRepository,
    private val deviceInfo: String,
) : BaseScreenModel<NewPasswordState, NewPasswordSideEffect, NewPasswordEvent>(
    initialState = NewPasswordState.initial(),
) {

    override fun onFetchData() = Unit

    override fun dispatch(event: NewPasswordEvent) {
        when (event) {
            is NewPasswordEvent.OnPasswordChanged -> updateState {
                it.copy(password = event.value, link = LinkState.Usable)
            }
            NewPasswordEvent.OnRevealToggled -> updateState { it.copy(isRevealed = !it.isRevealed) }
            NewPasswordEvent.OnSubmitClicked -> submit()
            NewPasswordEvent.OnRequestNewLinkClicked -> screenModelScope {
                postSideEffect(NewPasswordSideEffect.OpenRecovery)
            }
        }
    }

    private fun submit() {
        screenModelScope { current ->
            if (!current.isSubmitEnabled) return@screenModelScope
            updateState { it.copy(isSubmitting = true) }
            val saved = save(password = current.password)
            updateState { it.copy(isSubmitting = false) }
            when (saved) {
                is RequestResult.Success -> postSideEffect(NewPasswordSideEffect.PasswordChanged)
                is RequestResult.Error -> showFailure(saved)
            }
        }
    }

    private suspend fun save(password: String): RequestResult<Unit> {
        val emailToken = resetToken
        if (emailToken != null) {
            return authRepository.resetPasswordByEmail(
                token = emailToken,
                password = password,
                deviceInfo = deviceInfo,
            )
        }
        val telegramToken = claimToken
            ?: return RequestResult.Error(
                kind = RequestFailure.NotFound,
                statusCode = null,
                userMessage = "",
                devMessage = "neither a letter link nor a telegram claim was passed",
            )
        return authRepository.resetPasswordByTelegram(
            claimToken = telegramToken,
            password = password,
            deviceInfo = deviceInfo,
        )
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        when (failure.kind) {
            RequestFailure.Conflict -> updateState { it.copy(link = conflictMeaning()) }
            RequestFailure.Gone -> updateState { it.copy(link = LinkState.Expired) }
            RequestFailure.NotFound -> updateState { it.copy(link = LinkState.AlreadyUsed) }
            else -> postSideEffect(NewPasswordSideEffect.ShowFailure(failure))
        }
    }

    private fun conflictMeaning(): LinkState =
        if (resetToken != null) LinkState.AlreadyUsed else LinkState.NotConfirmedYet
}
