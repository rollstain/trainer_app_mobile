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
import app.trainer.app.PendingInvite
import app.trainer.app.SessionController
import app.trainer.app.SessionStatus
import app.trainer.base.failure.AppFailureState
import app.trainer.entities.RequestResult
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.NavContainer
import app.trainer.navigation.Screens
import app.trainer.navigation.rememberNavigator
import app.trainer.navigation.toEntries
import app.trainer.uikit.AppTheme
import app.trainer.uikit.backwardScreenTransition
import app.trainer.uikit.forwardScreenTransition
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppFullScreenProgress
import app.trainer.uikit.widgets.AppToastHost
import org.koin.compose.koinInject

@Composable
fun AppGate() {
    val sessionController: SessionController = koinInject()
    val status by sessionController.status.collectAsState()
    AppTheme {
        AppToastHost {
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
    LaunchedEffect(Unit) { pendingInvite.consume() }
    AppRoot(isCoach = isCoach)
}

@Composable
private fun NoCoachRoot() {
    val navigator = rememberNavigator(startKey = Screens.NoCoach)
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
    val invitedCode by pendingInvite.code.collectAsState()
    val startKey = invitedCode?.let { Screens.InviteLink(code = it) }
        ?: Screens.Invite(afterSessionExpiry = afterSessionExpiry)
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
