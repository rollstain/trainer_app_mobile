package app.trainer.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val CONFIRM_PATH_PREFIX = "/c/"
private const val CONFIRM_HOST = "confirm"

class PendingEmailConfirmation {

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
            withoutQuery.contains(CONFIRM_PATH_PREFIX) -> withoutQuery.substringAfterLast(CONFIRM_PATH_PREFIX)
            withoutQuery.contains("$CONFIRM_HOST/") -> withoutQuery.substringAfterLast('/')
            else -> return null
        }
        return token.takeIf { it.isNotBlank() }
    }
}
