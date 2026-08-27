package app.trainer.entities

enum class RequestFailure {
    Network,
    Unauthorized,
    Forbidden,
    NotFound,
    Conflict,
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
    ) : RequestResult<Nothing>
}
