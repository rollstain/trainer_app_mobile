package app.trainer.feature.account.telegramlink.mvi

import app.trainer.entities.RequestResult

sealed interface LinkStep {

    data object Offered : LinkStep

    data object Waiting : LinkStep

    data object Linked : LinkStep

    data object Failed : LinkStep
}

data class TelegramLinkState(
    val name: String,
    val step: LinkStep,
) {

    companion object {

        fun initial(name: String): TelegramLinkState = TelegramLinkState(
            name = name,
            step = LinkStep.Offered,
        )
    }
}

sealed interface TelegramLinkEvent {

    data object OnLinkClicked : TelegramLinkEvent

    data object OnSkipClicked : TelegramLinkEvent

    data object OnContinueClicked : TelegramLinkEvent
}

sealed interface TelegramLinkSideEffect {

    data class OpenTelegram(val deepLink: String) : TelegramLinkSideEffect

    data object Done : TelegramLinkSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : TelegramLinkSideEffect
}
