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
) {

    val isLastMethod: Boolean
        get() = methods.size == 1

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
}

sealed interface LoginMethodsSideEffect {

    data class OpenTelegram(val deepLink: String) : LoginMethodsSideEffect

    data object ShowLinked : LoginMethodsSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : LoginMethodsSideEffect
}
