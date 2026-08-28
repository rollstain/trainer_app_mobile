package app.trainer.feature.account.welcome.mvi

import app.trainer.data.auth.AuthProvider
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface TelegramLogin {

    data object Idle : TelegramLogin

    data object Starting : TelegramLogin

    data object Waiting : TelegramLogin

    data class Failed(val message: String) : TelegramLogin
}

data class WelcomeState(
    val afterSessionExpiry: Boolean,
    val providers: ImmutableList<AuthProvider>,
    val telegram: TelegramLogin,
    val isLoading: Boolean,
) {

    val hasProviders: Boolean
        get() = providers.isNotEmpty()

    companion object {

        fun initial(afterSessionExpiry: Boolean): WelcomeState = WelcomeState(
            afterSessionExpiry = afterSessionExpiry,
            providers = persistentListOf(),
            telegram = TelegramLogin.Idle,
            isLoading = true,
        )
    }
}

sealed interface WelcomeEvent {

    data object OnTelegramClicked : WelcomeEvent

    data object OnTelegramCancelled : WelcomeEvent

    data object OnCodeClicked : WelcomeEvent
}

sealed interface WelcomeSideEffect {

    data class OpenTelegram(val deepLink: String) : WelcomeSideEffect

    data object OpenCodeEntry : WelcomeSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : WelcomeSideEffect
}
