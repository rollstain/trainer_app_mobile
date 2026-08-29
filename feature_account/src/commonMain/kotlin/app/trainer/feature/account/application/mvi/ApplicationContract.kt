package app.trainer.feature.account.application.mvi

import app.trainer.entities.RequestResult

const val ABOUT_MIN_LENGTH = 40
const val ABOUT_MAX_LENGTH = 400
const val ABOUT_COUNTER_FROM = 100

data class ApplicationState(
    val displayName: String,
    val about: String,
    val isSending: Boolean,
    val isTooShortShown: Boolean,
    val isLoading: Boolean,
) {

    val isAboutCounterVisible: Boolean
        get() = about.length >= ABOUT_COUNTER_FROM

    val isSendEnabled: Boolean
        get() = displayName.isNotBlank() && about.trim().length >= ABOUT_MIN_LENGTH && !isSending

    companion object {

        fun initial(): ApplicationState = ApplicationState(
            displayName = "",
            about = "",
            isSending = false,
            isTooShortShown = false,
            isLoading = true,
        )
    }
}

sealed interface ApplicationEvent {

    data class OnDisplayNameChanged(val displayName: String) : ApplicationEvent

    data class OnAboutChanged(val about: String) : ApplicationEvent

    data object OnSendClicked : ApplicationEvent
}

sealed interface ApplicationSideEffect {

    data object Sent : ApplicationSideEffect

    data class ShowFailure(val failure: RequestResult.Error) : ApplicationSideEffect
}
