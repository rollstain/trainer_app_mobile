package app.trainer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.trainer.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val LOG_TAG = "navigation"

@Stable
class NavigationState(val stack: NavBackStack<NavKey>) {

    val currentKey by derivedStateOf { stack.lastOrNull() }

    val rootKey by derivedStateOf { stack.firstOrNull() }
}

@Stable
class Navigator(
    val state: NavigationState,
    private val parent: Navigator?,
    private val logger: Logger,
) {

    private val stack = state.stack
    private val mainImmediateScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    fun push(key: NavKey) {
        mutateStack(action = "push", key = key) {
            if (key == state.rootKey) {
                clearSubStack()
            } else {
                stack.remove(key)
                stack.add(key)
            }
        }
    }

    fun pop() {
        mutateStack(action = "pop") {
            when {
                stack.isEmpty() -> Unit
                stack.size > 1 -> {
                    ViewModelRegistry.remove(stack.last())
                    stack.removeAt(stack.lastIndex)
                }
                else -> parent?.pop()
            }
        }
    }

    fun replace(key: NavKey) {
        mutateStack(action = "replace", key = key) {
            when {
                stack.isEmpty() -> stack.add(key)
                key == state.rootKey -> clearSubStack()
                else -> {
                    ViewModelRegistry.remove(stack.last())
                    stack.removeAt(stack.lastIndex)
                    stack.add(key)
                }
            }
        }
    }

    fun replaceAll(key: NavKey) {
        mutateStack(action = "replaceAll", key = key) {
            val root = parent
            if (root != null) {
                root.replaceAll(key)
            } else {
                ViewModelRegistry.clearAll()
                stack.clear()
                stack.add(key)
            }
        }
    }

    fun popUntilRoot() {
        mutateStack(action = "popUntilRoot") { clearSubStack() }
    }

    fun popUntil(predicate: (NavKey) -> Boolean) {
        mutateStack(action = "popUntil") {
            if (stack.isEmpty()) return@mutateStack
            while (stack.size > 1 && !predicate(stack.last())) {
                ViewModelRegistry.remove(stack.last())
                stack.removeAt(stack.lastIndex)
            }
            if (!predicate(stack.last())) parent?.popUntil(predicate)
        }
    }

    inline fun <reified T : NavKey> popUntil() {
        popUntil { it is T }
    }

    fun <T> setResult(requestKey: ScreenRequestKey<T>, result: T?) {
        logger.info(tag = LOG_TAG, message = "setResult requestKey=$requestKey result=$result")
        ScreenResults.put(requestKey = requestKey, result = result)
    }

    fun <T> popWithResult(requestKey: ScreenRequestKey<T>, result: T?) {
        setResult(requestKey = requestKey, result = result)
        pop()
    }

    @Composable
    fun <T> handleResult(requestKey: ScreenRequestKey<T>, onResult: (T) -> Unit) {
        LaunchedEffect(requestKey) {
            val result = ScreenResults.take(requestKey)
            if (result != null) onResult(result)
        }
    }

    private fun clearSubStack() {
        if (stack.size <= 1) return
        val root = stack.first()
        stack.drop(1).forEach { ViewModelRegistry.remove(it) }
        stack.clear()
        stack.add(root)
    }

    private fun mutateStack(action: String, key: NavKey? = null, mutate: () -> Unit) {
        mainImmediateScope.launch {
            Snapshot.withMutableSnapshot {
                logger.info(tag = LOG_TAG, message = "$action key=$key stack=${stack.toList()}")
                mutate()
            }
        }
    }
}
