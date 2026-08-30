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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.base.failure.AppFailureState
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachAnswer
import app.trainer.feature.progress.presentation.formcheck.mvi.FormCheckRow
import app.trainer.feature.progress.presentation.formcheck.mvi.FormChecksEvent
import app.trainer.feature.progress.presentation.formcheck.mvi.FormChecksState
import app.trainer.feature.progress.presentation.formcheck.mvi.TooLargeVideo
import app.trainer.media.VideoPlayer
import app.trainer.strings.Res
import app.trainer.strings.form_checks_answer_approved
import app.trainer.strings.form_checks_awaiting
import app.trainer.strings.form_checks_empty_description
import app.trainer.strings.form_checks_empty_title
import app.trainer.strings.form_checks_send_action
import app.trainer.strings.form_checks_sending_hint
import app.trainer.strings.form_checks_title
import app.trainer.strings.form_checks_too_large_confirm
import app.trainer.strings.form_checks_too_large_description
import app.trainer.strings.form_checks_too_large_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppCardShimmerList
import app.trainer.uikit.widgets.AppConfirmDialog
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.TopBarLeading
import org.jetbrains.compose.resources.stringResource

private const val SHIMMER_CARDS = 3
private const val LOAD_MORE_CARDS = 1
private const val SHIMMER_CARD_LINES = 3
private const val VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun FormChecksView(
    modifier: Modifier = Modifier,
    state: FormChecksState,
    onEvent: (FormChecksEvent) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(
            title = stringResource(Res.string.form_checks_title),
            leading = TopBarLeading.Back(onClick = onBackClick),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when {
                state.failure != null -> AppFailureState(
                    failure = state.failure,
                    onRetry = { onEvent(FormChecksEvent.OnReloadRequested) },
                )
                state.isLoading -> AppCardShimmerList(count = SHIMMER_CARDS, lines = SHIMMER_CARD_LINES)
                state.checks.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = stringResource(Res.string.form_checks_empty_title),
                    description = stringResource(Res.string.form_checks_empty_description),
                    action = PlaceholderAction.None,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
                ) {
                    items(items = state.checks, key = FormCheckRow::formCheckId) { check ->
                        FormCheckCard(modifier = Modifier.animateItem(), check = check)
                    }
                    if (state.hasMore) {
                        item(key = "load-more") {
                            LaunchedEffect(state.nextCursor) { onEvent(FormChecksEvent.OnEndReached) }
                            AppCardShimmerList(count = LOAD_MORE_CARDS, lines = SHIMMER_CARD_LINES)
                        }
                    }
                }
            }
        }
        AppButton(
            modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
            text = stringResource(Res.string.form_checks_send_action),
            onClick = onSendClick,
            tone = ButtonTone.Primary,
            size = ButtonSize.Large,
            state = if (state.isSending) ButtonState.Loading else ButtonState.Idle,
        )
        if (state.isSending) {
            AppText(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.dp16, vertical = AppTheme.spacing.dp8),
                text = stringResource(Res.string.form_checks_sending_hint),
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
    val tooLarge = state.tooLargeVideo
    if (tooLarge != null) {
        TooLargeVideoDialog(video = tooLarge, onEvent = onEvent)
    }
}

@Composable
private fun TooLargeVideoDialog(video: TooLargeVideo, onEvent: (FormChecksEvent) -> Unit) {
    AppConfirmDialog(
        title = stringResource(Res.string.form_checks_too_large_title),
        description = stringResource(
            Res.string.form_checks_too_large_description,
            video.megabytes,
            video.limitMegabytes,
        ),
        confirmText = stringResource(Res.string.form_checks_too_large_confirm),
        onConfirm = { onEvent(FormChecksEvent.OnTooLargeVideoDismissed) },
        onDismissRequest = { onEvent(FormChecksEvent.OnTooLargeVideoDismissed) },
    )
}

@Composable
private fun FormCheckCard(modifier: Modifier = Modifier, check: FormCheckRow) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppText(
                text = check.dateLabel,
                style = AppTheme.typography.overline,
                color = AppTheme.colors.textMuted,
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
            when (val answer = check.answer) {
                CoachAnswer.Awaiting -> AppText(
                    text = stringResource(Res.string.form_checks_awaiting),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.warning,
                )
                CoachAnswer.Approved -> AppText(
                    text = stringResource(Res.string.form_checks_answer_approved),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.success,
                )
                is CoachAnswer.Comment -> AppText(
                    text = answer.text,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}
