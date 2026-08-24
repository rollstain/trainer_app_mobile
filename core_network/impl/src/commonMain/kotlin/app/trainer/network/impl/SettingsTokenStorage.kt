package app.trainer.network.impl

import app.trainer.network.AuthTokens
import app.trainer.network.TokenStorage
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val ACCESS_TOKEN_KEY = "accessToken"
private const val REFRESH_TOKEN_KEY = "refreshToken"

class SettingsTokenStorage(
    private val settings: Settings,
    private val ioDispatcher: CoroutineDispatcher,
) : TokenStorage {

    override suspend fun read(): AuthTokens? = withContext(ioDispatcher) {
        val accessToken = settings.getStringOrNull(ACCESS_TOKEN_KEY)
        val refreshToken = settings.getStringOrNull(REFRESH_TOKEN_KEY)
        if (accessToken == null || refreshToken == null) {
            null
        } else {
            AuthTokens(accessToken = accessToken, refreshToken = refreshToken)
        }
    }

    override suspend fun write(tokens: AuthTokens) {
        withContext(ioDispatcher) {
            settings.putString(ACCESS_TOKEN_KEY, tokens.accessToken)
            settings.putString(REFRESH_TOKEN_KEY, tokens.refreshToken)
        }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            settings.remove(ACCESS_TOKEN_KEY)
            settings.remove(REFRESH_TOKEN_KEY)
        }
    }
}
