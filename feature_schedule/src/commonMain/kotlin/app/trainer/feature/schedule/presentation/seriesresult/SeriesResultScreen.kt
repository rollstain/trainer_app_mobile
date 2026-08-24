package app.trainer.feature.schedule.presentation.seriesresult

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.trainer.data.schedule.CoachSlot
import app.trainer.data.schedule.SkippedSlot
import app.trainer.data.schedule.SlotSeriesResult
import app.trainer.feature.schedule.domain.SlotSeriesResults
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.uikit.AppTheme
import app.trainer.uikit.dashedBorder
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppText
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind
import org.koin.compose.koinInject

private const val TITLE = "Серия создана"
private const val CREATED_CAPTION = "Создано"
private const val SKIPPED_CAPTION = "Пропущено"
private const val SKIPPED_TITLE = "Пропущены — время уже занято"
private const val BACK_ACTION = "К календарю"
private const val EXPIRED_TITLE = "Результат недоступен"
private const val EXPIRED_DESCRIPTION = "Откройте календарь — созданные слоты уже там."

class SeriesResultScreen(private val batchId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val results: SlotSeriesResults = koinInject()
        val result = remember(batchId) { results.take(batchId) }

        Column(modifier = Modifier.fillMaxSize().screenBackground()) {
            AppTopBar(title = TITLE)
            if (result == null) {
                AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EXPIRED_TITLE,
                    description = EXPIRED_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = BACK_ACTION,
                        onClick = { navigator.replaceAll(Screens.CoachCalendar(weekStartIso = null)) },
                    ),
                )
            } else {
                ResultContent(
                    modifier = Modifier.weight(1f),
                    result = result,
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
                    text = BACK_ACTION,
                    onClick = { navigator.replaceAll(Screens.CoachCalendar(weekStartIso = null)) },
                    tone = ButtonTone.Primary,
                    size = ButtonSize.Large,
                )
            }
        }
    }
}

@Composable
private fun ResultContent(modifier: Modifier, result: SlotSeriesResult) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp12),
    ) {
        item(key = "summary") {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    caption = CREATED_CAPTION,
                    value = result.created.size.toString(),
                    isWarning = false,
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    caption = SKIPPED_CAPTION,
                    value = result.skipped.size.toString(),
                    isWarning = true,
                )
            }
        }
        if (result.skipped.isNotEmpty()) {
            item(key = "skipped") {
                SkippedBlock(skipped = result.skipped)
            }
        }
        items(items = result.created, key = CoachSlot::id) { slot ->
            AppText(
                text = slot.startsAt.toString(),
                style = AppTheme.typography.numeric,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, caption: String, value: String, isWarning: Boolean) {
    Column(
        modifier = modifier
            .background(
                color = AppTheme.colors.bgSurface,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp4),
    ) {
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

@Composable
private fun SkippedBlock(skipped: List<SkippedSlot>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTheme.colors.warningSoft,
                shape = RoundedCornerShape(AppTheme.radius.dp12),
            )
            .dashedBorder(color = AppTheme.colors.warning, cornerRadius = AppTheme.radius.dp12)
            .padding(AppTheme.spacing.dp16),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.dp8),
    ) {
        AppText(
            text = SKIPPED_TITLE,
            style = AppTheme.typography.bodyStrong,
            color = AppTheme.colors.warning,
        )
        skipped.forEach { slot ->
            AppText(
                text = slot.startsAt.toString(),
                style = AppTheme.typography.numeric,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}
