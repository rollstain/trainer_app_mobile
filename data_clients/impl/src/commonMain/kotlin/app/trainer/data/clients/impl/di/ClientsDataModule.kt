package app.trainer.data.clients.impl.di

import app.trainer.data.clients.ClientNotesRepository
import app.trainer.data.clients.ParticipantsRepository
import app.trainer.data.clients.impl.ClientNotesRepositoryImpl
import app.trainer.data.clients.impl.ClientsMapper
import app.trainer.data.clients.impl.ParticipantsRepositoryImpl
import org.koin.dsl.module

class ClientsDataModule {

    val module = module {
        single { ClientsMapper(logger = get()) }
        single<ParticipantsRepository> {
            ParticipantsRepositoryImpl(httpClientProvider = get(), mapper = get())
        }
        single<ClientNotesRepository> {
            ClientNotesRepositoryImpl(httpClientProvider = get(), mapper = get())
        }
    }
}
