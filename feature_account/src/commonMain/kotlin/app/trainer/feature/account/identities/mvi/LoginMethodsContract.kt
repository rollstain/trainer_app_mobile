package app.trainer.feature.account.identities.mvi

import app.trainer.data.auth.AuthProvider
import app.trainer.entities.RequestResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LoginMethodRow(
    val provider: AuthProvider,
    val linkedAtLabel: String,
)

sealed interface LinkProgress {

    data object Idle : LinkProgress

    data object Waiting : LinkProgress

    data class Failed(val message: String) : LinkProgress
}

data class LoginMethodsState(
    val methods: ImmutableList<LoginMethodRow>,
    val link: LinkProgress,
    val unlinking: AuthProvider?,
    val confirmedUnlink: AuthProvider?,
    val isLoading: Boolean,
    val failure: RequestResult.Error?,
    val hasPassword: Boolean,
    val passwordChangedLabel: String?,
    val email: String?,
    val emailConfirmed: Boolean,
    val isResendingConfirmation: Boolean,
) {

    val waysIn: Int
        get() = methods.size + if (hasPassword) 1 else 0

    val isLastMethod: Boolean
        get() = waysIn == 1

    val canLinkTelegram: Boolean
        get() = methods.none { it.provider == AuthProvider.TELEGRAM }

    companion object {

        fun initial(): LoginMethodsState = LoginMethodsState(
            methods = persistentListOf(),
            link = LinkProgress.Idle,
            unlinking = null,
            confirmedUnlink = null,
            isLoading = true,
            failure = null,
            hasPassword = false,
            passwordChangedLabel = null,
            email = null,
            emailConfirmed = true,
            isResendingConfirmation = false,
        )
    }
}

sealed interface LoginMethodsEvent {

    data object OnReloadRequested : LoginMethodsEvent

    data object OnLinkTelegramClicked : LoginMethodsEvent

    data object OnLinkCancelled : LoginMethodsEvent

    data class OnUnlinkClicked(val provider: AuthProvider) : LoginMethodsEvent

    data object OnUnlinkConfirmed : LoginMethodsEvent

    data object OnUnlinkDismissed : LoginMethodsEvent

    data object OnPasswordClicked : LoginMethodsEvent

    data object OnEmailClicked : LoginMethodsEvent

    data object OnResendConfirmationClicked : LoginMethodsEvent
}

sealed interface LoginMethodsSideEffect {

    data class OpenTelegram(val deepLink: String) : LoginMethodsSideEffect

    data object ShowLinked : LoginMethodsSideEffect

    data object OpenPasswordForm : LoginMethodsSideEffect

    data object OpenContactForm : LoginMethodsSideEffect

    data object ShowConfirmationSent : LoginMethodsSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : LoginMethodsSideEffect
}
