package app.trainer.logger

class ConsoleLogger : Logger {

    override fun info(tag: String, message: String) {
        println("[$tag] $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        println("[$tag] ERROR $message")
        throwable?.printStackTrace()
    }
}
