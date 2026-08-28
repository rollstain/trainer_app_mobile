package app.trainer.entities

data class Paged<T>(val items: T, val nextCursor: String?) {

    val hasMore: Boolean
        get() = nextCursor != null
}
