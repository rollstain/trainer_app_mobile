package app.trainer.feature.clientcard.presentation.people.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleEvent
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleState
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppButton
import app.trainer.uikit.widgets.AppCellShimmer
import app.trainer.uikit.widgets.AppListCell
import app.trainer.uikit.widgets.AppStatePlaceholder
import app.trainer.uikit.widgets.AppTopBar
import app.trainer.uikit.widgets.ButtonSize
import app.trainer.uikit.widgets.ButtonState
import app.trainer.uikit.widgets.ButtonTone
import app.trainer.uikit.widgets.PlaceholderAction
import app.trainer.uikit.widgets.PlaceholderKind

private const val SHIMMER_ROWS = 5
private const val TITLE = "Люди"
private const val INVITE_ACTION = "Создать приглашение"
private const val EMPTY_TITLE = "Подопечных пока нет"
private const val EMPTY_DESCRIPTION =
    "Отправьте приглашение — человек появится в списке сразу после входа."
private const val FAILURE_TITLE = "Не удалось загрузить"
private const val FAILURE_DESCRIPTION = "Проверьте соединение и попробуйте ещё раз."
private const val FAILURE_ACTION = "Повторить"

@Composable
fun PeopleView(
    modifier: Modifier = Modifier,
    state: PeopleState,
    onEvent: (PeopleEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().screenBackground()) {
        AppTopBar(title = TITLE)
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> LoadingList()
                state.isFailed -> AppStatePlaceholder(
                    kind = PlaceholderKind.Failure,
                    title = FAILURE_TITLE,
                    description = FAILURE_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = FAILURE_ACTION,
                        onClick = { onEvent(PeopleEvent.OnRetryClicked) },
                    ),
                )
                state.people.isEmpty() -> AppStatePlaceholder(
                    kind = PlaceholderKind.Empty,
                    title = EMPTY_TITLE,
                    description = EMPTY_DESCRIPTION,
                    action = PlaceholderAction.Button(
                        text = INVITE_ACTION,
                        onClick = { onEvent(PeopleEvent.OnCreateInviteClicked) },
                    ),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.people, key = { it.userId }) { person ->
                        AppListCell(
                            title = person.displayName,
                            onClick = { onEvent(PeopleEvent.OnPersonClicked(person.userId)) },
                        )
                    }
                }
            }
        }
        if (state.people.isNotEmpty()) {
            AppButton(
                modifier = Modifier.fillMaxWidth().padding(AppTheme.spacing.dp16),
                text = INVITE_ACTION,
                onClick = { onEvent(PeopleEvent.OnCreateInviteClicked) },
                tone = ButtonTone.Primary,
                size = ButtonSize.Large,
                state = if (state.isCreatingInvite) ButtonState.Loading else ButtonState.Idle,
            )
        }
    }
}

@Composable
private fun LoadingList() {
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(SHIMMER_ROWS) { index ->
            AppCellShimmer(isLastRow = index == SHIMMER_ROWS - 1)
        }
    }
}
