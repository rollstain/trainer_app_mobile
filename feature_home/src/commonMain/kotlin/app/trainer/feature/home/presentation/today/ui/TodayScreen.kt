package app.trainer.feature.home.presentation.today.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.home.presentation.today.mvi.TodayScreenModel
import app.trainer.feature.home.presentation.today.mvi.TodaySideEffect
import app.trainer.navigation.DiaryPeriod
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState

class TodayScreen : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: TodayScreenModel = koinScreenModel()
        val state by screenModel.collectAsState()

        TodayView(state = state, onEvent = { screenModel.dispatch(event = it) })

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: TodaySideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        TodaySideEffect.OpenProfile -> navigator.push(Screens.Profile)
        TodaySideEffect.OpenCalendar -> navigator.replaceAll(Screens.CoachCalendar(weekStartIso = null))
        TodaySideEffect.OpenChats -> navigator.replaceAll(Screens.CoachChats)
        TodaySideEffect.OpenSlotCreation -> navigator.push(Screens.NewSlot(dateIso = null))
        is TodaySideEffect.OpenClientCard -> navigator.push(Screens.ClientCard(effect.clientUserId))
        TodaySideEffect.OpenFormChecks -> navigator.push(Screens.CoachFormChecks)
        is TodaySideEffect.OpenDialog -> navigator.push(Screens.Chat(dialogId = effect.dialogId))
        is TodaySideEffect.OpenDiary -> navigator.push(
            Screens.CoachClientDiary(clientUserId = effect.clientUserId, period = DiaryPeriod.Month)
        )
        is TodaySideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
