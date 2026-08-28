package app.trainer.feature.progress.presentation.formcheck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.progress.presentation.formcheck.mvi.AwaitingFormCheck
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachFormChecksEvent
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachFormChecksState
import app.trainer.media.VideoPlayer
import app.trainer.strings.Res
import app.trainer.strings.form_checks_coach_empty_description
import app.trainer.strings.form_checks_coach_empty_title
import app.trainer.strings.form_checks_coach_title
import app.trainer.strings.form_checks_reply_action
import app.trainer.strings.form_checks_reply_hint
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTextField
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TextFieldKind
import app.trainer.uikit.widgets.TextFieldLabel
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val SHIMMER_CARD_LINES = 3
private const val VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun CoachFormChecksView(
    state: CoachFormChecksState,
    onEvent: (CoachFormChecksEvent) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.form_checks_coach_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(CoachFormChecksEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                state.checks.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.form_checks_coach_empty_title),
                    description = stringResource(Res.string.form_checks_coach_empty_description),
                    action = PlaceholderAction.None,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
                ) {
                    items(items = state.checks, key = AwaitingFormCheck::formCheckId) { check ->
                        AwaitingCard(modifier = Modifier.animateItem(), check = check, onEvent = onEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun AwaitingCard(
    modifier: Modifier = Modifier,
    check: AwaitingFormCheck,
    onEvent: (CoachFormChecksEvent) -> Unit,
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppText(
                text = check.clientDisplayName,
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.textPrimary,
            )
            AppText(
                text = listOfNotNull(check.dateLabel, check.exerciseName).joinToString(separator = " · "),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            check.videoUrl?.let { url ->
                VideoPlayer(
                    modifier = Modifier.fillMaxWidth().aspectRatio(VIDEO_ASPECT_RATIO),
                    url = url,
                )
            }
            check.note?.let { note ->
                AppText(text = note, style = AppTheme.typography.body, color = AppTheme.colors.textPrimary)
            }
            AppTextField(
                modifier = Modifier.fillMaxWidth(),
                value = check.draft,
                onValueChange = { text ->
                    onEvent(CoachFormChecksEvent.OnDraftChanged(formCheckId = check.formCheckId, text = text))
                },
                label = TextFieldLabel.Text(stringResource(Res.string.form_checks_reply_hint)),
                kind = TextFieldKind.Multiline,
            )
            AppButton(
                text = stringResource(Res.string.form_checks_reply_action),
                onClick = { onEvent(CoachFormChecksEvent.OnReplyClicked(check.formCheckId)) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Medium,
                state = if (check.isSending) ButtonState.Loading else ButtonState.Idle,
            )
        }
    }
}
