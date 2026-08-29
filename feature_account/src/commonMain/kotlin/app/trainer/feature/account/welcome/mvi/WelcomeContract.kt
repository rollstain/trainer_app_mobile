package app.trainer.feature.account.welcome.mvi

import app.trainer.entities.RequestResult

enum class LoginIntent { Client, Coach }

sealed interface TelegramLogin {

    data object Idle : TelegramLogin

    data object Starting : TelegramLogin

    data object Waiting : TelegramLogin

    data class Failed(val message: String) : TelegramLogin
}

data class WelcomeState(
    val afterSessionExpiry: Boolean,
    val telegram: TelegramLogin,
) {

    companion object {

        fun initial(afterSessionExpiry: Boolean): WelcomeState = WelcomeState(
            afterSessionExpiry = afterSessionExpiry,
            telegram = TelegramLogin.Idle,
        )
    }
}

sealed interface WelcomeEvent {

    data object OnTelegramClicked : WelcomeEvent

    data object OnCoachClicked : WelcomeEvent

    data object OnTelegramCancelled : WelcomeEvent

    data object OnCodeClicked : WelcomeEvent
}

sealed interface WelcomeSideEffect {

    data class OpenTelegram(val deepLink: String) : WelcomeSideEffect

    data object OpenCodeEntry : WelcomeSideEffect

    data object ShowCoachRequested : WelcomeSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : WelcomeSideEffect
}
