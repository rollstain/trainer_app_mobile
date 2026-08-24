package app.trainer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import app.trainer.logger.Logger
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.LocalKoinScopeContext
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.scope.Scope

val LocalNavigator: ProvidableCompositionLocal<Navigator?> = compositionLocalOf { null }

val ProvidableCompositionLocal<Navigator?>.currentOrThrow: Navigator
    @Composable get() = LocalNavigator.current ?: error("Navigator не установлен")

@OptIn(KoinExperimentalAPI::class)
@Composable
fun NavigationState.toEntries(): List<NavEntry<Any>> {
    return rememberDecoratedNavEntries(
        backStack = stack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = koinEntryProvider(),
    )
}

@Composable
fun rememberNavigator(startKey: NavKey): Navigator {
    val stack = rememberKoinNavBackStack(startKey)
    val state = remember(stack) { NavigationState(stack) }
    val parent = LocalNavigator.current
    val logger: Logger = koinInject()
    return remember(state, parent) { Navigator(state = state, parent = parent, logger = logger) }
}

@Composable
private fun rememberKoinNavBackStack(vararg elements: NavKey): NavBackStack<NavKey> {
    return rememberNavBackStack(
        configuration = koinNavConfigProvider(),
        elements = elements,
    )
}

@OptIn(KoinInternalApi::class)
@Composable
private fun koinNavConfigProvider(
    scope: Scope = LocalKoinScopeContext.current.getValue(),
): SavedStateConfiguration {
    val installers = scope.getAll<NavKeyProviderInstaller<out NavKey>>()
    return SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                installers.forEach { it.build(this) }
            }
        }
    }
}
