package app.trainer.feature.schedule.di

import app.trainer.feature.schedule.domain.ScheduleWeeks
import app.trainer.feature.schedule.domain.SlotSeriesResults
import app.trainer.feature.schedule.presentation.newslot.mvi.NewSlotScreenModel
import app.trainer.feature.schedule.presentation.newslot.ui.NewSlotScreen
import app.trainer.feature.schedule.presentation.seriesresult.SeriesResultScreen
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleScreenModel
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleScreenModel
import app.trainer.feature.schedule.presentation.client.ui.ClientScheduleScreen
import app.trainer.feature.schedule.presentation.coach.ui.CoachScheduleScreen
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class ScheduleFeatureModule {

    val module = module {
        single { ScheduleWeeks() }
        single { SlotSeriesResults() }
        viewModel { (dateIso: String) ->
            NewSlotScreenModel(
                initialDateIso = dateIso.ifEmpty {
                    Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
                },
                scheduleRepository = get(),
                profileRepository = get(),
                weeks = get(),
                seriesResults = get(),
            )
        }
        screen<Screens.NewSlot> { NewSlotScreen(dateIso = it.dateIso) }
        screen<Screens.SlotSeriesResult> { SeriesResultScreen(batchId = it.batchId) }
        screen<Screens.CoachCalendar> { CoachScheduleScreen() }
        screen<Screens.ClientBooking> { ClientScheduleScreen() }
        viewModel {
            CoachScheduleScreenModel(
                scheduleRepository = get(),
                profileRepository = get(),
                weeks = get(),
            )
        }
        viewModel {
            ClientScheduleScreenModel(
                scheduleRepository = get(),
                participantsRepository = get(),
                weeks = get(),
            )
        }
    }
}
