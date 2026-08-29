package app.trainer.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val RESET_PATH_PREFIX = "/r/"
private const val RESET_HOST = "reset"

class PendingPasswordReset {

    private val state = MutableStateFlow<String?>(null)

    val token: StateFlow<String?> = state.asStateFlow()

    fun remember(link: String) {
        tokenOf(link)?.let { state.value = it }
    }

    fun consume() {
        state.value = null
    }

    private fun tokenOf(link: String): String? {
        val withoutQuery = link.substringBefore('?').trimEnd('/')
        val token = when {
            withoutQuery.contains(RESET_PATH_PREFIX) -> withoutQuery.substringAfterLast(RESET_PATH_PREFIX)
            withoutQuery.contains("$RESET_HOST/") -> withoutQuery.substringAfterLast('/')
            else -> return null
        }
        return token.takeIf { it.isNotBlank() }
    }
}
