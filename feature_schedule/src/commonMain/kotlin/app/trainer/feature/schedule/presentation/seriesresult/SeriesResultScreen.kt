package app.trainer.feature.schedule.presentation.seriesresult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.trainer.base.failure.toastMessage
import app.trainer.feature.schedule.presentation.seriesresult.mvi.SeriesResultScreenModel
import app.trainer.feature.schedule.presentation.seriesresult.mvi.SeriesResultSideEffect
import app.trainer.feature.schedule.presentation.seriesresult.mvi.SeriesResultState
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.series_result_back_action
import app.trainer.strings.series_result_created_caption
import app.trainer.strings.series_result_expired_description
import app.trainer.strings.series_result_expired_title
import app.trainer.strings.series_result_failure_description
import app.trainer.strings.series_result_failure_title
import app.trainer.strings.series_result_skipped_caption
import app.trainer.strings.series_result_skipped_title
import app.trainer.strings.series_result_title
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCard
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.CardDecoration
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import app.trainer.uikit.widgets.ToastHostState
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf

class SeriesResultScreen(private val batchId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: SeriesResultScreenModel = koinScreenModel(
            parameters = { parametersOf(batchId) },
        )
        val state by screenModel.collectAsState()

        SeriesResultView(
            state = state,
            onBackToCalendar = {
                navigator.popUntil { it is Screens.CoachCalendar || it is Screens.CoachToday }
                navigator.selectRoot(Screens.CoachCalendar(weekStartIso = null))
            },
        )

        screenModel.collectSideEffect { effect ->
            when (effect) {
                is SeriesResultSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
            }
        }
    }
}

@Composable
private fun SeriesResultView(state: SeriesResultState, onBackToCalendar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().screenBackground().navigationBarsPadding()) {
        AppTopBar(title = stringResource(Res.string.series_result_title))
        when {
            state.isExpired -> AppStatePlaceholder(
                kind = PlaceholderKind.Empty,
                title = stringResource(Res.string.series_result_expired_title),
                description = stringResource(Res.string.series_result_expired_description),
                action = PlaceholderAction.Button(
                    text = stringResource(Res.string.series_result_back_action),
                    onClick = onBackToCalendar,
                ),
            )
            state.failure != null -> AppStatePlaceholder(
                kind = PlaceholderKind.Failure,
                title = stringResource(Res.string.series_result_failure_title),
                description = stringResource(Res.string.series_result_failure_description),
                action = PlaceholderAction.Button(
                    text = stringResource(Res.string.series_result_back_action),
                    onClick = onBackToCalendar,
                ),
            )
            else -> {
                ResultContent(modifier = Modifier.weight(1f), state = state)
                AppButton(
                    modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
                    text = stringResource(Res.string.series_result_back_action),
                    onClick = onBackToCalendar,
                    tone = ButtonTone.Primary,
                    size = ButtonSize.Large,
                )
            }
        }
    }
}

@Composable
private fun ResultContent(modifier: Modifier, state: SeriesResultState) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        item(key = "summary") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    caption = stringResource(Res.string.series_result_created_caption),
                    value = state.createdLabels.size.toString(),
                    isWarning = false,
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    caption = stringResource(Res.string.series_result_skipped_caption),
                    value = state.skippedLabels.size.toString(),
                    isWarning = true,
                )
            }
        }
        if (state.skippedLabels.isNotEmpty()) {
            item(key = "skipped") {
                SkippedBlock(labels = state.skippedLabels)
            }
        }
        items(items = state.createdLabels, key = { it }) { label ->
            AppText(
                text = label,
                style = AppTheme.typography.body,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, caption: String, value: String, isWarning: Boolean) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4)) {
            AppText(
                text = caption,
                style = AppTheme.typography.caption,
                color = AppTheme.colors.textSecondary,
            )
            AppText(
                text = value,
                style = AppTheme.typography.numericBig,
                color = if (isWarning) AppTheme.colors.warning else AppTheme.colors.success,
            )
        }
    }
}

@Composable
private fun SkippedBlock(labels: ImmutableList<String>) {
    AppCard(
        background = AppTheme.colors.warningSoft,
        decoration = CardDecoration.DashedOutline(AppTheme.colors.warning),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
            AppText(
                text = stringResource(Res.string.series_result_skipped_title),
                style = AppTheme.typography.bodyStrong,
                color = AppTheme.colors.warning,
            )
            labels.forEach { label ->
                AppText(
                    text = label,
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
    }
}
