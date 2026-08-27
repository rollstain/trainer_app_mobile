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

private const val COACH_TITLE_EN = "Chats"
private const val COACH_EMPTY_TITLE_EN = "No one here yet"
private const val COACH_INVITE_ACTION_EN = "Create an invite"
private const val CLIENT_TITLE_EN = "Chat"
private const val CLIENT_EMPTY_TITLE_EN = "No messages yet"

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = android.app.Application::class,
    qualifiers = "en-rUS-w411dp-h891dp-xhdpi",
)
class ChatListLocaleTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `coach screen is translated`() {
        compose.setContent { Screen(isCoach = true) }

        compose.onNodeWithText(COACH_TITLE_EN).assertIsDisplayed()
        compose.onNodeWithText(COACH_EMPTY_TITLE_EN).assertIsDisplayed()
        compose.onNodeWithText(COACH_INVITE_ACTION_EN).assertIsDisplayed()
    }

    @Test
    fun `client screen is translated and keeps the role split`() {
        compose.setContent { Screen(isCoach = false) }

        compose.onNodeWithText(CLIENT_TITLE_EN).assertIsDisplayed()
        compose.onNodeWithText(CLIENT_EMPTY_TITLE_EN).assertIsDisplayed()
        compose.onNodeWithText(COACH_INVITE_ACTION_EN).assertDoesNotExist()
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
