package app.trainer.network.impl

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

private const val TOKENS_KEYCHAIN_SERVICE = "app.trainer.tokens"

actual class SecureSettingsFactory {

    @OptIn(ExperimentalSettingsImplementation::class)
    actual fun create(): Settings = KeychainSettings(service = TOKENS_KEYCHAIN_SERVICE)
}
