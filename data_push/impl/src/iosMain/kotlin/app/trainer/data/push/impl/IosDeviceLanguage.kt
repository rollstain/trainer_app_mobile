package app.trainer.data.push.impl

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun deviceLanguage(): String = NSLocale.currentLocale.languageCode
