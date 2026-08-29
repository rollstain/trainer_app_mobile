package app.trainer.feature.account.contact.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.strings.Res
import app.trainer.strings.password_form_current_wrong
import org.jetbrains.compose.resources.getString

class ContactLinkScreenModel(
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<ContactLinkState, ContactLinkSideEffect, ContactLinkEvent>(
    initialState = ContactLinkState.initial(),
) {

    init {
        onFetchData()
    }

    override fun onFetchData() {
        onFetchDataScope {
            val profile = profileRepository.me()
            if (profile !is RequestResult.Success) return@onFetchDataScope
            updateState { it.copy(needsCurrentPassword = profile.data.hasPassword) }
        }
    }

    override fun dispatch(event: ContactLinkEvent) {
        when (event) {
            ContactLinkEvent.OnSubmitClicked -> save()
            ContactLinkEvent.OnSkipClicked -> dismiss()
            is ContactLinkEvent.OnKindChanged -> updateState {
                it.copy(kind = event.kind, value = "")
            }
            is ContactLinkEvent.OnValueChanged -> updateState { it.copy(value = event.value) }
            is ContactLinkEvent.OnCurrentPasswordChanged -> updateState {
                it.copy(currentPassword = event.value, currentPasswordError = null)
            }
        }
    }

    private fun save() {
        screenModelScope { state ->
            if (!state.isSubmitEnabled) return@screenModelScope
            updateState { it.copy(isSubmitting = true) }
            val contact = state.value.trim()
            val isPhone = state.kind == ContactKind.Phone
            val updated = profileRepository.updateContact(
                phone = if (isPhone) contact else null,
                email = if (isPhone) null else contact,
                currentPassword = state.currentPassword.takeIf { state.needsCurrentPassword },
            )
            updateState { it.copy(isSubmitting = false) }
            when (updated) {
                is RequestResult.Error -> showFailure(updated)
                is RequestResult.Success -> postSideEffect(ContactLinkSideEffect.Saved)
            }
        }
    }

    private suspend fun showFailure(failure: RequestResult.Error) {
        if (failure.kind != RequestFailure.Forbidden) {
            postSideEffect(ContactLinkSideEffect.ShowFailure(failure))
            return
        }
        val message = getString(Res.string.password_form_current_wrong)
        updateState { it.copy(currentPasswordError = message) }
    }

    private fun dismiss() {
        screenModelScope {
            postSideEffect(ContactLinkSideEffect.Dismissed)
        }
    }
}
