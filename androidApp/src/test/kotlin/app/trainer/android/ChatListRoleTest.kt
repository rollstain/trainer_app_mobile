package app.trainer.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontFamily
import app.trainer.feature.chat.presentation.list.mvi.ChatListState
import app.trainer.feature.chat.presentation.list.ui.ChatListView
import app.trainer.uikit.AppTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val COACH_INVITE_ACTION = "Создать приглашение"
private const val COACH_EMPTY_TITLE = "Пока никого нет"
private const val CLIENT_EMPTY_TITLE = "Переписки пока нет"
private const val COACH_SCREEN_TITLE = "Чаты"
private const val CLIENT_SCREEN_TITLE = "Чат"

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = android.app.Application::class,
    qualifiers = "ru-rRU-w411dp-h891dp-xhdpi",
)
class ChatListRoleTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `coach sees the invite action on an empty list`() {
        compose.setContent { Screen(isCoach = true) }

        compose.onNodeWithText(COACH_SCREEN_TITLE).assertIsDisplayed()
        compose.onNodeWithText(COACH_EMPTY_TITLE).assertIsDisplayed()
        compose.onNodeWithText(COACH_INVITE_ACTION).assertIsDisplayed()
    }

    @Test
    fun `client never sees the coach invite copy`() {
        compose.setContent { Screen(isCoach = false) }

        compose.onNodeWithText(CLIENT_SCREEN_TITLE).assertIsDisplayed()
        compose.onNodeWithText(CLIENT_EMPTY_TITLE).assertIsDisplayed()
        compose.onNodeWithText(COACH_INVITE_ACTION).assertDoesNotExist()
        compose.onNodeWithText(COACH_EMPTY_TITLE).assertDoesNotExist()
    }

    @androidx.compose.runtime.Composable
    private fun Screen(isCoach: Boolean) {
        AppTheme(textFontFamily = FontFamily.Default, numericFontFamily = FontFamily.Monospace) {
            ChatListView(
                state = ChatListState.initial().copy(
                    dialogs = persistentListOf(),
                    isCoach = isCoach,
                    isLoading = false,
                ),
                onEvent = {},
            )
        }
    }
}
