package app.trainer.android

import app.trainer.logger.Logger
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsLogger(
    private val delegate: Logger,
    private val crashlytics: FirebaseCrashlytics,
) : Logger {

    override fun info(tag: String, message: String) {
        delegate.info(tag = tag, message = message)
        crashlytics.log("[$tag] $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        delegate.error(tag = tag, message = message, throwable = throwable)
        crashlytics.log("[$tag] ERROR $message")
        crashlytics.recordException(throwable ?: NonFatalFailure(tag = tag, message = message))
    }
}

private class NonFatalFailure(tag: String, message: String) : Throwable("[$tag] $message")
