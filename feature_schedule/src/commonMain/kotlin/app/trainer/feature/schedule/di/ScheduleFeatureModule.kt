package app.trainer.feature.schedule.di

import app.trainer.feature.schedule.domain.SlotSeriesResults
import app.trainer.feature.schedule.presentation.client.mvi.ClientScheduleScreenModel
import app.trainer.feature.schedule.presentation.client.ui.ClientScheduleScreen
import app.trainer.feature.schedule.presentation.coach.mvi.CoachScheduleScreenModel
import app.trainer.feature.schedule.presentation.coach.ui.CoachScheduleScreen
import app.trainer.feature.schedule.presentation.groupsession.mvi.GroupSessionScreenModel
import app.trainer.feature.schedule.presentation.groupsession.ui.GroupSessionScreen
import app.trainer.feature.schedule.presentation.newslot.mvi.NewSlotScreenModel
import app.trainer.feature.schedule.presentation.newslot.ui.NewSlotScreen
import app.trainer.feature.schedule.presentation.seriesresult.SeriesResultScreen
import app.trainer.feature.schedule.presentation.seriesresult.mvi.SeriesResultScreenModel
import app.trainer.navigation.Screens
import app.trainer.navigation.screen
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class ScheduleFeatureModule {

    val module = module {
        single { SlotSeriesResults() }
        viewModel { (dateIso: String) ->
            NewSlotScreenModel(
                initialDateIso = dateIso.ifEmpty {
                    Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
                },
                scheduleRepository = get(),
                clientsRepository = get(),
                profileRepository = get(),
                weeks = get(),
                seriesResults = get(),
            )
        }
        screen<Screens.NewSlot> { NewSlotScreen(dateIso = it.dateIso) }
        viewModel { (batchId: String) ->
            SeriesResultScreenModel(
                batchId = batchId,
                seriesResults = get(),
                profileRepository = get(),
                weeks = get(),
            )
        }
        screen<Screens.SlotSeriesResult> { SeriesResultScreen(batchId = it.batchId) }
        screen<Screens.CoachCalendar> { CoachScheduleScreen() }
        screen<Screens.GroupSession> { GroupSessionScreen(slotId = it.slotId) }
        viewModel { (slotId: String) ->
            GroupSessionScreenModel(
                slotId = slotId,
                scheduleRepository = get(),
                participantsRepository = get(),
                profileRepository = get(),
                weeks = get(),
            )
        }
        screen<Screens.ClientBooking> { ClientScheduleScreen() }
        viewModel {
            CoachScheduleScreenModel(
                scheduleRepository = get(),
                clientsRepository = get(),
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
