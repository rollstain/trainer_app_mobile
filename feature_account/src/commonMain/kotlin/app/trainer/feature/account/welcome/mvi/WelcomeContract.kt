package app.trainer.feature.account.welcome.mvi

import app.trainer.entities.RequestResult
import app.trainer.feature.account.telegram.TelegramLoginState

data class WelcomeState(
    val afterSessionExpiry: Boolean,
    val telegram: TelegramLoginState,
) {

    companion object {

        fun initial(afterSessionExpiry: Boolean): WelcomeState = WelcomeState(
            afterSessionExpiry = afterSessionExpiry,
            telegram = TelegramLoginState.Idle,
        )
    }
}

sealed interface WelcomeEvent {

    data object OnSignInClicked : WelcomeEvent

    data object OnSignUpClicked : WelcomeEvent

    data object OnTelegramClicked : WelcomeEvent

    data object OnTelegramCancelled : WelcomeEvent

    data object OnCodeClicked : WelcomeEvent
}

sealed interface WelcomeSideEffect {

    data object OpenSignIn : WelcomeSideEffect

    data object OpenSignUp : WelcomeSideEffect

    data class OpenTelegram(val deepLink: String) : WelcomeSideEffect

    data object OpenCodeEntry : WelcomeSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : WelcomeSideEffect
}
