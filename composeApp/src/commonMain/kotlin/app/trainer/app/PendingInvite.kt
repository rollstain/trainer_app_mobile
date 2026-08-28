package app.trainer.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val INVITE_PATH_PREFIX = "/i/"
private const val INVITE_HOST = "invite"

class PendingInvite {

    private val state = MutableStateFlow<String?>(null)

    val code: StateFlow<String?> = state.asStateFlow()

    fun remember(link: String) {
        codeOf(link)?.let { state.value = it }
    }

    fun consume() {
        state.value = null
    }

    private fun codeOf(link: String): String? {
        val withoutQuery = link.substringBefore('?').trimEnd('/')
        val code = when {
            withoutQuery.contains(INVITE_PATH_PREFIX) -> withoutQuery.substringAfterLast(INVITE_PATH_PREFIX)
            withoutQuery.contains("$INVITE_HOST/") -> withoutQuery.substringAfterLast('/')
            else -> return null
        }
        return code.takeIf { it.isNotBlank() }?.uppercase()
    }
}
