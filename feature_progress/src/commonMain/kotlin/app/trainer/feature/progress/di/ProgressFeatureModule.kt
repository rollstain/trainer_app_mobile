package app.trainer.feature.progress.di

import app.trainer.feature.progress.presentation.checkin.mvi.CheckInScreenModel
import app.trainer.feature.progress.presentation.checkin.mvi.CoachCheckInsScreenModel
import app.trainer.feature.progress.presentation.checkin.ui.CheckInScreen
import app.trainer.feature.progress.presentation.checkin.ui.CoachCheckInsScreen
import app.trainer.feature.progress.presentation.formcheck.mvi.CoachFormChecksScreenModel
import app.trainer.feature.progress.presentation.formcheck.mvi.FormChecksScreenModel
import app.trainer.feature.progress.presentation.formcheck.ui.CoachFormChecksScreen
import app.trainer.feature.progress.presentation.formcheck.ui.FormChecksScreen
import app.trainer.feature.progress.presentation.photos.mvi.PhotoCompareScreenModel
import app.trainer.feature.progress.presentation.photos.mvi.PhotoOwner
import app.trainer.feature.progress.presentation.photos.ui.PhotoCompareScreen
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
        screen<Screens.PhotoCompare> { key ->
            PhotoCompareScreen(
                owner = key.clientUserId?.let(PhotoOwner::Client) ?: PhotoOwner.Own,
            )
        }
        screen<Screens.FormChecks> { FormChecksScreen() }
        screen<Screens.CoachFormChecks> { CoachFormChecksScreen() }
        screen<Screens.CoachCheckIns> { CoachCheckInsScreen() }
        viewModel { FormChecksScreenModel(formCheckRepository = get()) }
        viewModel { CoachFormChecksScreenModel(formCheckRepository = get()) }
        viewModel { CoachCheckInsScreenModel(checkInRepository = get()) }
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
        viewModel { (owner: PhotoOwner) ->
            PhotoCompareScreenModel(owner = owner, checkInRepository = get())
        }
    }
}
