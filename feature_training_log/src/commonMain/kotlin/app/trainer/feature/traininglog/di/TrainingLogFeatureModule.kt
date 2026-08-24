package app.trainer.feature.traininglog.di

import app.trainer.feature.traininglog.domain.DurationInput
import app.trainer.feature.traininglog.domain.RestTimer
import app.trainer.feature.traininglog.domain.VolumeFormat
import app.trainer.feature.traininglog.presentation.coach.mvi.CoachTrainingLogScreenModel
import app.trainer.feature.traininglog.presentation.coach.ui.CoachTrainingLogScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorScreenModel
import app.trainer.feature.traininglog.presentation.editor.ui.TrainingLogEditorScreen
import app.trainer.feature.traininglog.presentation.newexercise.NewExerciseScreen
import app.trainer.feature.traininglog.presentation.newexercise.NewExerciseScreenModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class TrainingLogFeatureModule {

    val module = module {
        single { DurationInput() }
        single { RestTimer(alarm = get(), restIntervalStore = get()) }
        single { VolumeFormat() }
        screen<Screens.CoachClientDiary> { CoachTrainingLogScreen(clientUserId = it.clientUserId) }
        screen<Screens.ClientDiaryDay> { TrainingLogEditorScreen(entryDateIso = it.dateIso) }
        screen<Screens.NewExercise> { NewExerciseScreen() }
        viewModel { NewExerciseScreenModel(trainingLogRepository = get()) }
        viewModel { (entryDateIso: String) ->
            TrainingLogEditorScreenModel(
                entryDateIso = entryDateIso,
                trainingLogRepository = get(),
                trainingInputStore = get(),
                weightInput = get(),
                durationInput = get(),
                volumeFormat = get(),
                restTimer = get(),
            )
        }
        viewModel { (clientUserId: String) ->
            CoachTrainingLogScreenModel(
                clientUserId = clientUserId,
                trainingLogRepository = get(),
                weightInput = get(),
                durationInput = get(),
                volumeFormat = get(),
            )
        }
    }
}
