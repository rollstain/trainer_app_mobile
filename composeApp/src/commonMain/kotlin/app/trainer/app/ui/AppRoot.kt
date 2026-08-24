package app.trainer.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.trainer.network.HttpClientProvider
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.NavContainer
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screens
import app.trainer.navigation.rememberNavigator
import app.trainer.navigation.toEntries
import app.trainer.uikit.AppTheme
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppBottomNavigation
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppToastHost
import app.trainer.uikit.widgets.BottomNavItem
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import org.koin.compose.koinInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun AppRoot(isCoach: Boolean) {
    val httpClientProvider: HttpClientProvider = koinInject()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = { httpClientProvider.plainClient })) }
            .build()
    }
    AppTheme {
        AppToastHost {
            val startKey = if (isCoach) Screens.CoachChats else Screens.ClientBooking(
                coachId = null,
                weekStartIso = null,
            )
            val navigator = rememberNavigator(startKey = startKey)
            CompositionLocalProvider(LocalNavigator provides navigator) {
                AppNavigationScaffold(navigator = navigator, isCoach = isCoach)
            }
        }
    }
}

@Composable
private fun AppNavigationScaffold(navigator: Navigator, isCoach: Boolean) {
    val entries = navigator.state.toEntries()
    val currentKey by navigatorCurrentKey(navigator)
    val tabs = if (isCoach) coachTabs() else clientTabs()
    val selectedTabId = tabs.firstOrNull { it.id == currentKey }?.id ?: tabs.first().id
    val isRootScreen = tabs.any { it.id == currentKey }

    Column(modifier = Modifier.fillMaxSize().screenBackground()) {
        Box(modifier = Modifier.weight(1f)) {
            NavContainer(entries = entries, onBack = navigator::pop)
        }
        if (isRootScreen) {
            AppBottomNavigation(
                items = tabs,
                selectedId = selectedTabId,
                onSelect = { tabId -> navigator.replaceAll(rootKeyOf(tabId)) },
            )
        }
    }
}

@Composable
private fun navigatorCurrentKey(navigator: Navigator) = androidx.compose.runtime.remember(navigator) {
    androidx.compose.runtime.derivedStateOf {
        navigator.state.currentKey?.let(::tabIdOf)
    }
}

private const val TAB_CHATS = "chats"
private const val TAB_CALENDAR = "calendar"
private const val TAB_DIARIES = "diaries"
private const val TAB_PEOPLE = "people"
private const val TAB_BOOKING = "booking"
private const val TAB_DIARY = "diary"
private const val TAB_PROGRESS = "progress"

@Composable
private fun coachTabs(): List<BottomNavItem> = listOf(
    BottomNavItem(id = TAB_CHATS, label = "Чаты", icon = { AppIcons.chats(it) }),
    BottomNavItem(id = TAB_CALENDAR, label = "Календарь", icon = { AppIcons.calendar(it) }),
    BottomNavItem(id = TAB_DIARIES, label = "Дневники", icon = { AppIcons.logs(it) }),
    BottomNavItem(id = TAB_PEOPLE, label = "Люди", icon = { AppIcons.people(it) }),
)

@Composable
private fun clientTabs(): List<BottomNavItem> = listOf(
    BottomNavItem(id = TAB_CHATS, label = "Чат", icon = { AppIcons.chats(it) }),
    BottomNavItem(id = TAB_BOOKING, label = "Запись", icon = { AppIcons.calendar(it) }),
    BottomNavItem(id = TAB_DIARY, label = "Дневник", icon = { AppIcons.logs(it) }),
    BottomNavItem(id = TAB_PROGRESS, label = "Прогресс", icon = { AppIcons.progress(it) }),
)

private fun tabIdOf(key: Any): String? = when (key) {
    is Screens.CoachChats -> TAB_CHATS
    is Screens.CoachCalendar -> TAB_CALENDAR
    is Screens.CoachDiaries -> TAB_DIARIES
    is Screens.CoachPeople -> TAB_PEOPLE
    is Screens.ClientBooking -> TAB_BOOKING
    is Screens.ClientDiaryDay -> TAB_DIARY
    is Screens.Progress -> TAB_PROGRESS
    else -> null
}

private fun rootKeyOf(tabId: String): Screens = when (tabId) {
    TAB_CHATS -> Screens.CoachChats
    TAB_CALENDAR -> Screens.CoachCalendar(weekStartIso = null)
    TAB_DIARIES -> Screens.CoachDiaries
    TAB_PEOPLE -> Screens.CoachPeople
    TAB_BOOKING -> Screens.ClientBooking(coachId = null, weekStartIso = null)
    TAB_DIARY -> Screens.ClientDiaryDay(dateIso = todayIso())
    TAB_PROGRESS -> Screens.Progress
    else -> Screens.CoachChats
}

private fun todayIso(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
