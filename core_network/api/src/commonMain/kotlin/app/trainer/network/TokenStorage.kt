package app.trainer.network

interface TokenStorage {

    suspend fun read(): AuthTokens?

    suspend fun write(tokens: AuthTokens)

    suspend fun clear()
}
