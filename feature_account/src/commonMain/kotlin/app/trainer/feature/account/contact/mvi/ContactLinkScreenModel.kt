package app.trainer.feature.account.contact.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.profile.ProfileRepository
import app.trainer.entities.RequestResult

class ContactLinkScreenModel(
    private val profileRepository: ProfileRepository,
) : BaseScreenModel<ContactLinkState, ContactLinkSideEffect, ContactLinkEvent>(
    initialState = ContactLinkState.initial(),
) {

    override fun onFetchData() = Unit

    override fun dispatch(event: ContactLinkEvent) {
        when (event) {
            ContactLinkEvent.OnSubmitClicked -> save()
            ContactLinkEvent.OnSkipClicked -> finish()
            is ContactLinkEvent.OnKindChanged -> updateState {
                it.copy(kind = event.kind, value = "")
            }
            is ContactLinkEvent.OnValueChanged -> updateState { it.copy(value = event.value) }
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
            )
            updateState { it.copy(isSubmitting = false) }
            when (updated) {
                is RequestResult.Error -> postSideEffect(ContactLinkSideEffect.ShowFailure(updated))
                is RequestResult.Success -> postSideEffect(ContactLinkSideEffect.Finish)
            }
        }
    }

    private fun finish() {
        screenModelScope {
            postSideEffect(ContactLinkSideEffect.Finish)
        }
    }
}
