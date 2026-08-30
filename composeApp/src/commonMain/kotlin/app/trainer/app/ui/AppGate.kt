package app.trainer.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.app.PendingEmailConfirmation
import app.trainer.app.PendingInvite
import app.trainer.app.PendingPasswordReset
import app.trainer.app.SessionController
import app.trainer.app.SessionStatus
import app.trainer.base.failure.AppFailureState
import app.trainer.base.failure.toastMessage
import app.trainer.data.auth.FreshSignUp
import app.trainer.data.auth.IdentitiesRepository
import app.trainer.entities.RequestFailure
import app.trainer.entities.RequestResult
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.NavContainer
import app.trainer.navigation.Screens
import app.trainer.navigation.rememberNavigator
import app.trainer.navigation.toEntries
import app.trainer.strings.Res
import app.trainer.strings.email_confirm_expired
import app.trainer.strings.email_confirm_success
import app.trainer.strings.email_confirm_used
import app.trainer.uikit.AppTheme
import app.trainer.uikit.backwardScreenTransition
import app.trainer.uikit.forwardScreenTransition
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppFullScreenProgress
import app.trainer.uikit.widgets.AppToastHost
import app.trainer.uikit.widgets.LocalToastHost
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject

@Composable
fun AppGate() {
    val sessionController: SessionController = koinInject()
    val status by sessionController.status.collectAsState()
    AppTheme {
        AppToastHost {
            HandleEmailConfirmation()
            when (val current = status) {
                SessionStatus.Loading -> AppFullScreenProgress()
                is SessionStatus.SignedOut -> OnboardingRoot(afterSessionExpiry = current.afterSessionExpiry)
                is SessionStatus.ProfileUnavailable -> ProfileUnavailable(
                    failure = current.failure,
                    onRetry = sessionController::retry,
                )
                is SessionStatus.SignedIn -> if (current.isCoach || current.hasCoach) {
                    SignedInRoot(isCoach = current.isCoach)
                } else {
                    NoCoachRoot()
                }
            }
        }
    }
}

@Composable
private fun SignedInRoot(isCoach: Boolean) {
    val pendingInvite: PendingInvite = koinInject()
    val pendingPasswordReset: PendingPasswordReset = koinInject()
    LaunchedEffect(Unit) {
        pendingInvite.consume()
        pendingPasswordReset.consume()
    }
    AppRoot(isCoach = isCoach)
}

@Composable
private fun HandleEmailConfirmation() {
    val pendingEmailConfirmation: PendingEmailConfirmation = koinInject()
    val identitiesRepository: IdentitiesRepository = koinInject()
    val toastHost = LocalToastHost.current
    val token by pendingEmailConfirmation.token.collectAsState()
    LaunchedEffect(token) {
        val current = token ?: return@LaunchedEffect
        val confirmed = identitiesRepository.confirmEmail(current)
        toastHost.show(confirmationMessage(confirmed))
        pendingEmailConfirmation.consume()
    }
}

private suspend fun confirmationMessage(confirmed: RequestResult<Unit>): String = when (confirmed) {
    is RequestResult.Success -> getString(Res.string.email_confirm_success)
    is RequestResult.Error -> when (confirmed.kind) {
        RequestFailure.Conflict -> getString(Res.string.email_confirm_used)
        RequestFailure.Gone -> getString(Res.string.email_confirm_expired)
        else -> confirmed.toastMessage()
    }
}

@Composable
private fun NoCoachRoot() {
    val freshSignUp: FreshSignUp = koinInject()
    val freshName by freshSignUp.name.collectAsState()
    val startKey = if (freshName == null) Screens.NoCoach else Screens.TelegramLink
    val navigator = rememberNavigator(startKey = startKey)
    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(modifier = Modifier.fillMaxSize().screenBackground()) {
            NavContainer(
                entries = navigator.state.toEntries(),
                onBack = navigator::pop,
                forward = forwardScreenTransition(),
                backward = backwardScreenTransition(),
            )
        }
    }
}

@Composable
private fun OnboardingRoot(afterSessionExpiry: Boolean) {
    val pendingInvite: PendingInvite = koinInject()
    val pendingPasswordReset: PendingPasswordReset = koinInject()
    val invitedCode by pendingInvite.code.collectAsState()
    val resetToken by pendingPasswordReset.token.collectAsState()
    val startKey = resetToken?.let { Screens.NewPassword(resetToken = it, claimToken = null) }
        ?: invitedCode?.let { Screens.InviteLink(code = it) }
        ?: Screens.Welcome(afterSessionExpiry = afterSessionExpiry)
    val navigator = rememberNavigator(startKey = startKey)
    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(modifier = Modifier.fillMaxSize().screenBackground()) {
            NavContainer(
                entries = navigator.state.toEntries(),
                onBack = navigator::pop,
                forward = forwardScreenTransition(),
                backward = backwardScreenTransition(),
            )
        }
    }
}

@Composable
private fun ProfileUnavailable(failure: RequestResult.Error, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().screenBackground(),
        contentAlignment = Alignment.Center,
    ) {
        AppFailureState(failure = failure, onRetry = onRetry)
    }
}
