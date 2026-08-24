package app.trainer.feature.clientcard.di

import app.trainer.feature.clientcard.presentation.mvi.ClientCardScreenModel
import app.trainer.feature.clientcard.presentation.people.mvi.PeopleScreenModel
import app.trainer.feature.clientcard.presentation.people.ui.DiariesScreen
import app.trainer.feature.clientcard.presentation.people.ui.PeopleScreen
import app.trainer.feature.clientcard.presentation.ui.ClientCardScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class ClientCardFeatureModule {

    val module = module {
        screen<Screens.ClientCard> { ClientCardScreen(clientUserId = it.clientUserId) }
        screen<Screens.CoachPeople> { PeopleScreen() }
        screen<Screens.CoachDiaries> { DiariesScreen() }
        viewModel {
            PeopleScreenModel(participantsRepository = get(), authRepository = get())
        }
        viewModel { (clientUserId: String) ->
            ClientCardScreenModel(
                clientUserId = clientUserId,
                notesRepository = get(),
                checkInRepository = get(),
                habitsRepository = get(),
                weightInput = get(),
            )
        }
    }
}
