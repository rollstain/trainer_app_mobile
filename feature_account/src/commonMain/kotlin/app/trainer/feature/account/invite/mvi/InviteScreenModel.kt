package app.trainer.feature.account.invite.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.invite_code_not_found_message
import org.jetbrains.compose.resources.getString

private const val INVITE_CODE_LENGTH = 6

class InviteScreenModel(
    private val authRepository: AuthRepository,
    private val deviceInfo: String,
) : BaseScreenModel<InviteState, InviteSideEffect, InviteEvent>(
    initialState = InviteState.initial(prefilledCode = null),
) {

    override fun onFetchData() = Unit

    override fun dispatch(event: InviteEvent) {
        when (event) {
            InviteEvent.OnSubmitClicked -> redeem()
            is InviteEvent.OnCodeChanged -> updateState { current ->
                current.copy(
                    code = event.code.uppercase().take(INVITE_CODE_LENGTH),
                    codeError = null,
                )
            }
            is InviteEvent.OnDisplayNameChanged -> updateState {
                it.copy(displayName = event.displayName)
            }
        }
    }

    private fun redeem() {
        screenModelScope { state ->
            if (!state.isSubmitEnabled) return@screenModelScope
            updateState { it.copy(isSubmitting = true, codeError = null) }
            val redeemed = authRepository.redeemInvite(
                code = state.code,
                displayName = state.displayName.trim(),
                deviceInfo = deviceInfo,
            )
            updateState { it.copy(isSubmitting = false) }
            when (redeemed) {
                is RequestResult.Error -> {
                    val codeError = getString(Res.string.invite_code_not_found_message)
                    updateState { it.copy(codeError = codeError) }
                    postSideEffect(InviteSideEffect.ShowFailure(redeemed))
                }
                is RequestResult.Success -> postSideEffect(InviteSideEffect.OpenContactLink)
            }
        }
    }
}
