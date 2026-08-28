package app.trainer.feature.account.invitelink.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.account.invitelink.mvi.InviteLinkContent
import app.trainer.feature.account.invitelink.mvi.InviteLinkEvent
import app.trainer.feature.account.invitelink.mvi.InviteLinkProblem
import app.trainer.feature.account.invitelink.mvi.InviteLinkState
import app.trainer.strings.Res
import app.trainer.strings.invite_link_access_description
import app.trainer.strings.invite_link_access_unlink
import app.trainer.strings.invite_link_code_action
import app.trainer.strings.invite_link_expired_description
import app.trainer.strings.invite_link_expired_title
import app.trainer.strings.invite_link_join_action
import app.trainer.strings.invite_link_not_found_description
import app.trainer.strings.invite_link_not_found_title
import app.trainer.strings.invite_link_title
import app.trainer.strings.invite_link_used_description
import app.trainer.strings.invite_link_used_title
import app.trainer.strings.invite_link_wrong_coach_action
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppFullScreenProgress
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import org.jetbrains.compose.resources.stringResource

@Composable
fun InviteLinkView(
    modifier: Modifier = Modifier,
    state: InviteLinkState,
    onEvent: (InviteLinkEvent) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .screenBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                start = AppTheme.spacing.dp24,
                end = AppTheme.spacing.dp24,
                top = AppTheme.spacing.dp32,
                bottom = AppTheme.spacing.dp24,
            ),
    ) {
        when (val content = state.content) {
            InviteLinkContent.Loading -> AppFullScreenProgress()
            is InviteLinkContent.Failure -> Centered {
                AppFailureState(
                    failure = content.failure,
                    onRetry = { onEvent(InviteLinkEvent.OnReloadRequested) },
                )
            }
            is InviteLinkContent.Problem -> Centered {
                AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = stringResource(titleOf(content.kind)),
                    description = stringResource(descriptionOf(content.kind)),
                    action = PlaceholderAction.Button(
                        text = stringResource(Res.string.invite_link_code_action),
                        onClick = { onEvent(InviteLinkEvent.OnCodeEntryClicked) },
                    ),
                )
            }
            is InviteLinkContent.Coach -> CoachInvite(
                coachDisplayName = content.displayName,
                isJoining = state.isJoining,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun CoachInvite(coachDisplayName: String, isJoining: Boolean, onEvent: (InviteLinkEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        AppAvatar(displayName = coachDisplayName, size = AvatarSize.Hero)
        AppText(
            text = stringResource(Res.string.invite_link_title),
            style = AppTheme.typography.body,
            color = AppTheme.colors.textSecondary,
        )
        AppText(
            text = coachDisplayName,
            style = AppTheme.typography.display,
            color = AppTheme.colors.textPrimary,
        )
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                AppText(
                    text = stringResource(Res.string.invite_link_access_description),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textPrimary,
                )
                AppText(
                    text = stringResource(Res.string.invite_link_access_unlink),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.invite_link_join_action),
            onClick = { onEvent(InviteLinkEvent.OnJoinClicked) },
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = if (isJoining) ButtonState.Loading else ButtonState.Idle,
        )
        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.invite_link_wrong_coach_action),
            onClick = { onEvent(InviteLinkEvent.OnCodeEntryClicked) },
            tone = ButtonTone.Text,
            size = ButtonSize.Medium,
        )
    }
}

private fun titleOf(problem: InviteLinkProblem) = when (problem) {
    InviteLinkProblem.Expired -> Res.string.invite_link_expired_title
    InviteLinkProblem.AlreadyUsed -> Res.string.invite_link_used_title
    InviteLinkProblem.NotFound -> Res.string.invite_link_not_found_title
}

private fun descriptionOf(problem: InviteLinkProblem) = when (problem) {
    InviteLinkProblem.Expired -> Res.string.invite_link_expired_description
    InviteLinkProblem.AlreadyUsed -> Res.string.invite_link_used_description
    InviteLinkProblem.NotFound -> Res.string.invite_link_not_found_description
}
