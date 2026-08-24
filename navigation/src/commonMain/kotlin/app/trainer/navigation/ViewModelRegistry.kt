package app.trainer.navigation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

internal object ViewModelRegistry {

    private val stores = mutableMapOf<Any, ViewModelStore>()

    fun getOrCreate(key: Any): ViewModelStoreOwner {
        val store = stores.getOrPut(key) { ViewModelStore() }
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    fun remove(key: Any) {
        stores.remove(key.toString())?.clear()
    }

    fun clearAll() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}
