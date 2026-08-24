package app.trainer.logger

interface Logger {

    fun info(tag: String, message: String)

    fun error(tag: String, message: String, throwable: Throwable? = null)
}
