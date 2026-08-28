package app.trainer.feature.account.invite.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.AuthRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.invite_code_expired_message
import app.trainer.strings.invite_code_not_found_message
import app.trainer.strings.invite_code_used_message
import app.trainer.uikit.widgets.CODE_LENGTH
import org.jetbrains.compose.resources.getString

class InviteScreenModel(
    afterSessionExpiry: Boolean,
    private val authRepository: AuthRepository,
    private val deviceInfo: String,
) : BaseScreenModel<InviteState, InviteSideEffect, InviteEvent>(
    initialState = InviteState.initial(afterSessionExpiry = afterSessionExpiry),
) {

    override fun onFetchData() = Unit

    override fun dispatch(event: InviteEvent) {
        when (event) {
            InviteEvent.OnSubmitClicked -> check()
            is InviteEvent.OnCodeChanged -> changeCode(event.code)
        }
    }

    private fun changeCode(code: String) {
        val isPasted = code.length - state.code.length > 1
        updateState { it.copy(code = code, codeError = null) }
        if (isPasted && code.length == CODE_LENGTH) {
            check()
        }
    }

    private fun check() {
        screenModelScope { current ->
            if (!current.isSubmitEnabled) return@screenModelScope
            updateState { it.copy(isChecking = true, codeError = null) }
            when (val preview = authRepository.previewInvite(current.code)) {
                is RequestResult.Error -> showCodeError(preview)
                is RequestResult.Success -> if (preview.data.needsDisplayName) {
                    updateState { it.copy(isChecking = false) }
                    postSideEffect(
                        InviteSideEffect.OpenOnboarding(
                            code = current.code,
                            coachDisplayName = preview.data.coachDisplayName,
                        )
                    )
                } else {
                    redeemWithoutName(current.code)
                }
            }
        }
    }

    private suspend fun redeemWithoutName(code: String) {
        val redeemed = authRepository.redeemInvite(code = code, displayName = "", deviceInfo = deviceInfo)
        updateState { it.copy(isChecking = false) }
        when (redeemed) {
            is RequestResult.Error -> showCodeError(redeemed)
            is RequestResult.Success -> postSideEffect(InviteSideEffect.SignedIn)
        }
    }

    private suspend fun showCodeError(failure: RequestResult.Error) {
        val message = when (failure.kind) {
            RequestFailure.Gone -> getString(Res.string.invite_code_expired_message)
            RequestFailure.Conflict -> getString(Res.string.invite_code_used_message)
            RequestFailure.NotFound -> getString(Res.string.invite_code_not_found_message)
            else -> null
        }
        if (message == null) {
            updateState { it.copy(isChecking = false) }
            postSideEffect(InviteSideEffect.ShowFailure(failure))
            return
        }
        val keepsTypedCode = failure.kind == RequestFailure.NotFound
        updateState { current ->
            current.copy(
                isChecking = false,
                codeError = message,
                code = if (keepsTypedCode) current.code else "",
            )
        }
    }
}
