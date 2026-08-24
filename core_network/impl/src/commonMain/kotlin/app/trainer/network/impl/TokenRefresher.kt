package app.trainer.network.impl

import app.trainer.network.AuthTokens

interface TokenRefresher {

    suspend fun refresh(current: AuthTokens): AuthTokens?
}
