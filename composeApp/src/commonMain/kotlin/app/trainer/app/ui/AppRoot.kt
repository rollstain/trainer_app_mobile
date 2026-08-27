package app.trainer.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.NavContainer
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screens
import app.trainer.navigation.rememberNavigator
import app.trainer.navigation.toEntries
import app.trainer.network.HttpClientProvider
import app.trainer.strings.Res
import app.trainer.strings.tab_booking
import app.trainer.strings.tab_calendar
import app.trainer.strings.tab_client_chat
import app.trainer.strings.tab_coach_chats
import app.trainer.strings.tab_diaries
import app.trainer.strings.tab_diary
import app.trainer.strings.tab_next
import app.trainer.strings.tab_people
import app.trainer.strings.tab_progress
import app.trainer.strings.tab_today
import app.trainer.uikit.backwardScreenTransition
import app.trainer.uikit.forwardScreenTransition
import app.trainer.uikit.screenBackground
import app.trainer.uikit.widgets.AppBottomNavigation
import app.trainer.uikit.widgets.AppIcons
import app.trainer.uikit.widgets.AppToastHost
import app.trainer.uikit.widgets.BottomNavItem
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val IMAGE_MEMORY_CACHE_SHARE = 0.25
private const val IMAGE_DISK_CACHE_BYTES = 128L * 1024 * 1024

@Composable
fun AppRoot(isCoach: Boolean) {
    val httpClientProvider: HttpClientProvider = koinInject()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = { httpClientProvider.plainClient })) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context = context, percent = IMAGE_MEMORY_CACHE_SHARE)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(imageCacheDirectory(context))
                    .maxSizeBytes(IMAGE_DISK_CACHE_BYTES)
                    .build()
            }
            .build()
    }
    val startKey = if (isCoach) Screens.CoachToday else Screens.ClientNext
    key(isCoach) {
        val navigator = rememberNavigator(startKey = startKey)
        CompositionLocalProvider(LocalNavigator provides navigator) {
            AppNavigationScaffold(navigator = navigator, isCoach = isCoach)
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
        AppToastHost(modifier = Modifier.weight(1f)) {
            NavContainer(
                entries = entries,
                onBack = navigator::pop,
                forward = forwardScreenTransition(),
                backward = backwardScreenTransition(),
            )
        }
        if (isRootScreen) {
            AppBottomNavigation(
                items = tabs,
                selectedId = selectedTabId,
                onSelect = { tabId ->
                    if (tabId != selectedTabId) navigator.replaceAll(rootKeyOf(tabId))
                },
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

private const val TAB_TODAY = "today"
private const val TAB_NEXT = "next"
private const val TAB_CHATS = "chats"
private const val TAB_CALENDAR = "calendar"
private const val TAB_DIARIES = "diaries"
private const val TAB_PEOPLE = "people"
private const val TAB_BOOKING = "booking"
private const val TAB_DIARY = "diary"
private const val TAB_PROGRESS = "progress"

@Composable
private fun coachTabs(): List<BottomNavItem> = listOf(
    BottomNavItem(id = TAB_TODAY, label = stringResource(Res.string.tab_today), icon = { AppIcons.home(it) }),
    BottomNavItem(id = TAB_CHATS, label = stringResource(Res.string.tab_coach_chats), icon = { AppIcons.chats(it) }),
    BottomNavItem(id = TAB_CALENDAR, label = stringResource(Res.string.tab_calendar), icon = { AppIcons.calendar(it) }),
    BottomNavItem(id = TAB_DIARIES, label = stringResource(Res.string.tab_diaries), icon = { AppIcons.logs(it) }),
    BottomNavItem(id = TAB_PEOPLE, label = stringResource(Res.string.tab_people), icon = { AppIcons.people(it) }),
)

@Composable
private fun clientTabs(): List<BottomNavItem> = listOf(
    BottomNavItem(id = TAB_NEXT, label = stringResource(Res.string.tab_next), icon = { AppIcons.home(it) }),
    BottomNavItem(id = TAB_CHATS, label = stringResource(Res.string.tab_client_chat), icon = { AppIcons.chats(it) }),
    BottomNavItem(id = TAB_BOOKING, label = stringResource(Res.string.tab_booking), icon = { AppIcons.calendar(it) }),
    BottomNavItem(id = TAB_DIARY, label = stringResource(Res.string.tab_diary), icon = { AppIcons.logs(it) }),
    BottomNavItem(id = TAB_PROGRESS, label = stringResource(Res.string.tab_progress), icon = { AppIcons.progress(it) }),
)

private fun tabIdOf(key: Any): String? = when (key) {
    is Screens.CoachToday -> TAB_TODAY
    is Screens.ClientNext -> TAB_NEXT
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
    TAB_TODAY -> Screens.CoachToday
    TAB_NEXT -> Screens.ClientNext
    TAB_CHATS -> Screens.CoachChats
    TAB_CALENDAR -> Screens.CoachCalendar(weekStartIso = null)
    TAB_DIARIES -> Screens.CoachDiaries
    TAB_PEOPLE -> Screens.CoachPeople
    TAB_BOOKING -> Screens.ClientBooking(coachId = null, weekStartIso = null)
    TAB_DIARY -> Screens.ClientDiaryDay(dateIso = todayIso())
    TAB_PROGRESS -> Screens.Progress
    else -> Screens.CoachToday
}

private fun todayIso(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
