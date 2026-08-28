package app.trainer.feature.home.di

import app.trainer.feature.home.presentation.next.mvi.NextScreenModel
import app.trainer.feature.home.presentation.next.ui.NextScreen
import app.trainer.feature.home.presentation.today.mvi.TodayScreenModel
import app.trainer.feature.home.presentation.today.ui.TodayScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class HomeFeatureModule {

    val module = module {
        viewModel {
            TodayScreenModel(
                scheduleRepository = get(),
                chatRepository = get(),
                participantsRepository = get(),
                trainingLogRepository = get(),
                profileRepository = get(),
                checkInRepository = get(),
                formCheckRepository = get(),
                weeks = get(),
            )
        }
        screen<Screens.CoachToday> { TodayScreen() }
        viewModel {
            NextScreenModel(
                participantsRepository = get(),
                profileRepository = get(),
                scheduleRepository = get(),
                trainingLogRepository = get(),
                checkInRepository = get(),
                habitsRepository = get(),
                programRepository = get(),
                weightInput = get(),
                weeks = get(),
            )
        }
        screen<Screens.ClientNext> { NextScreen() }
    }
}
