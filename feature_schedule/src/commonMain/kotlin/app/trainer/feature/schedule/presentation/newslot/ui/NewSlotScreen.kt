package app.trainer.feature.schedule.presentation.newslot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.base.failure.toastMessage
import app.trainer.feature.schedule.presentation.newslot.mvi.NewSlotScreenModel
import app.trainer.feature.schedule.presentation.newslot.mvi.NewSlotSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.ScreenRequestKey
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.strings.Res
import app.trainer.strings.new_slot_slot_created_message
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.jetbrains.compose.resources.getString
import org.koin.core.parameter.parametersOf

internal val NEW_SLOT_REQUEST = ScreenRequestKey<Unit>("newSlot")

class NewSlotScreen(private val dateIso: String?) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val toastHost: ToastHostState = LocalToastHost.current
        val screenModel: NewSlotScreenModel = koinScreenModel(
            parameters = { parametersOf(dateIso.orEmpty()) },
        )
        val state by screenModel.collectAsState()

        NewSlotView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )

        screenModel.collectSideEffect { effect ->
            handleSideEffect(effect = effect, navigator = navigator, toastHost = toastHost)
        }
    }
}

private suspend fun handleSideEffect(
    effect: NewSlotSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        NewSlotSideEffect.SlotCreated -> {
            toastHost.show(getString(Res.string.new_slot_slot_created_message))
            navigator.popWithResult(NEW_SLOT_REQUEST, Unit)
        }
        is NewSlotSideEffect.SeriesCreated -> navigator.replace(
            Screens.SlotSeriesResult(batchId = effect.batchId)
        )
        is NewSlotSideEffect.ShowFailure -> toastHost.show(effect.failure.toastMessage())
    }
}
