package app.trainer.data.chat.impl.di

import app.trainer.data.chat.ChatRealtime
import app.trainer.data.chat.ChatRepository
import app.trainer.data.chat.impl.ChatLocalStore
import app.trainer.data.chat.impl.ChatMapper
import app.trainer.data.chat.impl.ChatRealtimeImpl
import app.trainer.data.chat.impl.ChatRepositoryImpl
import app.trainer.entities.LocalDataCleaner
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

class ChatDataModule {

    val module = module {
        single { ChatMapper(logger = get()) }
        single { ChatLocalStore(database = get(), mapper = get(), ioDispatcher = get()) }
        single<ChatRepository> {
            ChatRepositoryImpl(
                httpClientProvider = get(),
                localStore = get(),
                mapper = get(),
                presignedUploader = get(),
            )
        } bind LocalDataCleaner::class
        single<ChatRealtime> {
            ChatRealtimeImpl(
                httpClientProvider = get(),
                chatRepository = get(),
                tokenStorage = get(),
                mapper = get(),
                localStore = get(),
                json = get(),
                webSocketUrl = get(named(WEB_SOCKET_URL_QUALIFIER)),
                logger = get(),
                ioDispatcher = get(),
            )
        }
    }

    companion object {

        const val WEB_SOCKET_URL_QUALIFIER = "chatWebSocketUrl"
    }
}
