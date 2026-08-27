package app.trainer.network

import io.ktor.client.HttpClient

interface HttpClientProvider {

    val client: HttpClient

    val plainClient: HttpClient

    fun forgetAuthenticatedUser()
}
