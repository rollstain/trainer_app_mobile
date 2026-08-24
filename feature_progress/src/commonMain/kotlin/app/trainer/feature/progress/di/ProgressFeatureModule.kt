package app.trainer.feature.progress.di

import app.trainer.feature.progress.presentation.checkin.mvi.CheckInScreenModel
import app.trainer.feature.progress.presentation.checkin.ui.CheckInScreen
import app.trainer.feature.progress.presentation.progress.mvi.ProgressScreenModel
import app.trainer.feature.progress.presentation.progress.ui.ProgressScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class ProgressFeatureModule {

    val module = module {
        screen<Screens.Progress> { ProgressScreen() }
        screen<Screens.CheckIn> { CheckInScreen(dateIso = it.dateIso) }
        viewModel {
            ProgressScreenModel(
                checkInRepository = get(),
                habitsRepository = get(),
                weightInput = get(),
            )
        }
        viewModel { (dateIso: String) ->
            CheckInScreenModel(
                dateIso = dateIso,
                checkInRepository = get(),
                weightInput = get(),
            )
        }
    }
}
