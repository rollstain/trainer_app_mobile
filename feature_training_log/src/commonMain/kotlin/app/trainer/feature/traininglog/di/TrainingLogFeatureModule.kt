package app.trainer.feature.traininglog.di

import app.trainer.base.format.VolumeFormat
import app.trainer.feature.traininglog.domain.DurationInput
import app.trainer.feature.traininglog.domain.RestTimer
import app.trainer.feature.traininglog.presentation.coach.mvi.CoachTrainingLogScreenModel
import app.trainer.feature.traininglog.presentation.coach.ui.CoachTrainingLogScreen
import app.trainer.feature.traininglog.presentation.editor.mvi.TrainingLogEditorScreenModel
import app.trainer.feature.traininglog.presentation.editor.ui.TrainingLogEditorScreen
import app.trainer.feature.traininglog.presentation.library.mvi.ExerciseLibraryScreenModel
import app.trainer.feature.traininglog.presentation.library.ui.ExerciseLibraryScreen
import app.trainer.feature.traininglog.presentation.newexercise.NewExerciseScreen
import app.trainer.feature.traininglog.presentation.newexercise.NewExerciseScreenModel
import app.trainer.feature.traininglog.presentation.programday.mvi.ProgramDayScreenModel
import app.trainer.feature.traininglog.presentation.programday.ui.ProgramDayScreen
import app.trainer.feature.traininglog.presentation.programeditor.mvi.ProgramEditorScreenModel
import app.trainer.feature.traininglog.presentation.programeditor.ui.ProgramEditorScreen
import app.trainer.feature.traininglog.presentation.programs.mvi.ProgramsScreenModel
import app.trainer.feature.traininglog.presentation.programs.ui.ProgramsScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
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
        screen<Screens.ExerciseLibrary> { ExerciseLibraryScreen() }
        screen<Screens.Programs> { ProgramsScreen() }
        screen<Screens.ProgramEditor> { ProgramEditorScreen(programId = it.programId) }
        screen<Screens.ProgramDay> {
            ProgramDayScreen(
                programId = it.programId,
                weekNumber = it.weekNumber,
                dayOfWeek = it.dayOfWeek,
            )
        }
        viewModel { ProgramsScreenModel(programRepository = get()) }
        viewModel { (programId: String) ->
            ProgramEditorScreenModel(programId = programId, programRepository = get())
        }
        viewModel { (programId: String, weekNumber: Int, dayOfWeek: Int) ->
            ProgramDayScreenModel(
                programId = programId,
                weekNumber = weekNumber,
                dayOfWeek = dayOfWeek,
                programRepository = get(),
                trainingLogRepository = get(),
                weightInput = get(),
            )
        }
        viewModel { NewExerciseScreenModel(trainingLogRepository = get()) }
        viewModel { ExerciseLibraryScreenModel(trainingLogRepository = get()) }
        viewModel { (entryDateIso: String) ->
            TrainingLogEditorScreenModel(
                entryDateIso = entryDateIso,
                trainingLogRepository = get(),
                trainingInputStore = get(),
                weightInput = get(),
                durationInput = get(),
                volumeFormat = get(),
                restTimer = get(),
                programRepository = get(),
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
