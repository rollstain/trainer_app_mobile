package app.trainer.data.push.impl

import java.util.Locale

actual fun deviceLanguage(): String = Locale.getDefault().language
