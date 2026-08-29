package app.trainer.feature.account.providers

import app.trainer.data.auth.AuthProvider

internal fun providerNameOf(provider: AuthProvider): String = when (provider) {
    AuthProvider.TELEGRAM -> "Telegram"
    AuthProvider.VK -> "VK ID"
    AuthProvider.YANDEX -> "Яндекс ID"
    AuthProvider.APPLE -> "Apple"
    AuthProvider.GOOGLE -> "Google"
}

internal fun providerNameOf(rawProvider: String): String =
    AuthProvider.entries.firstOrNull { it.name == rawProvider }?.let(::providerNameOf) ?: rawProvider
