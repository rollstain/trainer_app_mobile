package app.trainer.feature.account.coachcard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import app.trainer.feature.account.coachcard.mvi.CoachCardScreenModel
import app.trainer.navigation.LocalNavigator
import app.trainer.navigation.Navigator
import app.trainer.navigation.Screen
import app.trainer.navigation.currentOrThrow
import app.trainer.navigation.koinScreenModel
import org.koin.core.parameter.parametersOf

class CoachCardScreen(private val coachId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val screenModel: CoachCardScreenModel = koinScreenModel(
            parameters = { parametersOf(coachId) },
        )
        val state by screenModel.collectAsState()

        CoachCardView(
            state = state,
            onEvent = { screenModel.dispatch(event = it) },
            onBackClick = navigator::pop,
        )
    }
}
