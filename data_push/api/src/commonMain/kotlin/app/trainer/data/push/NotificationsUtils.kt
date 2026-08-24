package app.trainer.data.push

enum class PushPlatform { ANDROID, IOS }

interface NotificationsUtils {

    val platform: PushPlatform

    suspend fun getMessagingToken(): String?
}

interface MessagingTokenManager {

    suspend fun refreshToken()
}
