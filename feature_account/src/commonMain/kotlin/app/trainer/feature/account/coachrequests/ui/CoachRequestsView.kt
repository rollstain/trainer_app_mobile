package app.trainer.feature.account.coachrequests.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.account.coachrequests.mvi.CoachRequestRow
import app.trainer.feature.account.coachrequests.mvi.CoachRequestsEvent
import app.trainer.feature.account.coachrequests.mvi.CoachRequestsState
import app.trainer.strings.Res
import app.trainer.strings.coach_requests_approve
import app.trainer.strings.coach_requests_asked_at
import app.trainer.strings.coach_requests_decline
import app.trainer.strings.coach_requests_empty_description
import app.trainer.strings.coach_requests_empty_title
import app.trainer.strings.coach_requests_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppAvatar
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.AvatarSize
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 2
private const val SHIMMER_CARD_LINES = 2

@Composable
fun CoachRequestsView(
    modifier: Modifier = Modifier,
    state: CoachRequestsState,
    onEvent: (CoachRequestsEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.coach_requests_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(CoachRequestsEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                state.requests.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.coach_requests_empty_title),
                    description = stringResource(Res.string.coach_requests_empty_description),
                    action = PlaceholderAction.None,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
                ) {
                    items(items = state.requests, key = { it.requestId }) { request ->
                        RequestCard(
                            request = request,
                            isDeciding = state.decidingId == request.requestId,
                            onEvent = onEvent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: CoachRequestRow,
    isDeciding: Boolean,
    onEvent: (CoachRequestsEvent) -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppAvatar(displayName = request.displayName, size = AvatarSize.Medium)
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = request.displayName,
                        style = AppTheme.typography.bodyStrong,
                        color = AppTheme.colors.textPrimary,
                    )
                    AppText(
                        text = stringResource(Res.string.coach_requests_asked_at, request.askedAtLabel),
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
            ) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.coach_requests_approve),
                    onClick = { onEvent(CoachRequestsEvent.OnApproved(request.requestId)) },
                    tone = ButtonTone.Primary,
                    size = ButtonSize.Medium,
                    state = if (isDeciding) ButtonState.Loading else ButtonState.Idle,
                )
                AppButton(
                    text = stringResource(Res.string.coach_requests_decline),
                    onClick = { onEvent(CoachRequestsEvent.OnDeclined(request.requestId)) },
                    tone = ButtonTone.Secondary,
                    size = ButtonSize.Medium,
                    state = if (isDeciding) ButtonState.Disabled else ButtonState.Idle,
                )
            }
        }
    }
}
