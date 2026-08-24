package app.trainer.navigation

internal object ScreenResults {

    private val results = mutableMapOf<String, Any?>()

    fun <T> take(requestKey: ScreenRequestKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return results.remove(requestKey.keyValue) as? T
    }

    fun <T> put(requestKey: ScreenRequestKey<T>, result: T?) {
        results[requestKey.keyValue] = result
    }
}
