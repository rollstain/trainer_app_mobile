package app.trainer.entities

sealed interface RequestResult<out T> {

    data class Success<out T>(val data: T) : RequestResult<T>

    data class Error(
        val statusCode: Int?,
        val userMessage: String,
        val devMessage: String,
    ) : RequestResult<Nothing>
}
