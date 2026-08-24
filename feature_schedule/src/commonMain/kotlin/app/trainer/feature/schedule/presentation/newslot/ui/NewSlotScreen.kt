package app.trainer.feature.schedule.presentation.newslot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.schedule.presentation.newslot.mvi.NewSlotScreenModel
import app.trainer.feature.schedule.presentation.newslot.mvi.NewSlotSideEffect
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.Screens
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import app.trainer.uikit.widgets.LocalToastHost
import app.trainer.uikit.widgets.ToastHostState
import org.koin.core.parameter.parametersOf

private const val SLOT_CREATED_MESSAGE = "Слот создан"

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

private fun handleSideEffect(
    effect: NewSlotSideEffect,
    navigator: Navigator,
    toastHost: ToastHostState,
) {
    when (effect) {
        NewSlotSideEffect.SlotCreated -> {
            toastHost.show(SLOT_CREATED_MESSAGE)
            navigator.pop()
        }
        is NewSlotSideEffect.SeriesCreated -> navigator.replace(
            Screens.SlotSeriesResult(batchId = effect.batchId)
        )
        is NewSlotSideEffect.ShowFailure -> toastHost.show(effect.failure.userMessage)
    }
}
