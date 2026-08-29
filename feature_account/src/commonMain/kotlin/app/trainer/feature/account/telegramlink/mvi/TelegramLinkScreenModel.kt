package app.trainer.feature.account.telegramlink.mvi

import app.trainer.base.BaseScreenModel
import app.trainer.data.auth.FreshSignUp
import app.trainer.entities.RequestResult
import app.trainer.feature.account.telegram.TelegramConfirmation
import kotlinx.coroutines.Job

class TelegramLinkScreenModel(
    private val freshSignUp: FreshSignUp,
    private val telegramConfirmation: TelegramConfirmation,
) : BaseScreenModel<TelegramLinkState, TelegramLinkSideEffect, TelegramLinkEvent>(
    initialState = TelegramLinkState.initial(name = freshSignUp.name.value.orEmpty()),
) {

    private var linkJob: Job? = null

    override fun onFetchData() = Unit

    override fun dispatch(event: TelegramLinkEvent) {
        when (event) {
            TelegramLinkEvent.OnLinkClicked -> link()
            TelegramLinkEvent.OnSkipClicked, TelegramLinkEvent.OnContinueClicked -> finish()
        }
    }

    private fun finish() {
        linkJob?.cancel()
        freshSignUp.consume()
        screenModelScope { postSideEffect(TelegramLinkSideEffect.Done) }
    }

    private fun link() {
        linkJob?.cancel()
        linkJob = screenModelScope {
            updateState { it.copy(step = LinkStep.Waiting) }
            when (val started = telegramConfirmation.start()) {
                is RequestResult.Error -> failed(started)
                is RequestResult.Success -> {
                    postSideEffect(TelegramLinkSideEffect.OpenTelegram(deepLink = started.data.deepLink))
                    awaitLink(claimToken = started.data.claimToken)
                }
            }
        }
    }

    private suspend fun awaitLink(claimToken: String) {
        val linked = telegramConfirmation.awaitLink(claimToken = claimToken) { }
        when (linked) {
            is RequestResult.Success -> updateState { it.copy(step = LinkStep.Linked) }
            is RequestResult.Error -> failed(linked)
        }
    }

    private suspend fun failed(failure: RequestResult.Error) {
        updateState { it.copy(step = LinkStep.Failed) }
        postSideEffect(TelegramLinkSideEffect.ShowFailure(failure))
    }
}
