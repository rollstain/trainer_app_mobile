package app.trainer.network.impl

import com.russhwolf.settings.Settings

expect class SecureSettingsFactory {

    fun create(): Settings
}
