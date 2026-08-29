package app.trainer.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FreshSignUp {

    private val state = MutableStateFlow<String?>(null)

    val name: StateFlow<String?> = state.asStateFlow()

    fun remember(displayName: String) {
        state.value = displayName.trim()
    }

    fun consume() {
        state.value = null
    }
}
