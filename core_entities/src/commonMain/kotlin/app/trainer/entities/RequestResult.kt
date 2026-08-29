package app.trainer.entities

enum class RequestFailure {
    Network,
    Unauthorized,
    Forbidden,
    NotFound,
    Conflict,
    Gone,
    TooManyRequests,
    Validation,
    Server,
    Parsing,
    Unknown,
}

sealed interface RequestResult<out T> {

    data class Success<out T>(val data: T) : RequestResult<T>

    data class Error(
        val kind: RequestFailure,
        val statusCode: Int?,
        val userMessage: String,
        val devMessage: String,
        val retryAfterSeconds: Long? = null,
        val fieldErrors: Map<String, String> = emptyMap(),
    ) : RequestResult<Nothing>
}
