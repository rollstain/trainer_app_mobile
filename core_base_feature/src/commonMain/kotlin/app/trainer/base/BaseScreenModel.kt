package app.trainer.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trainer.logger.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val LOG_TAG = "screen"

abstract class BaseScreenModel<STATE : Any, SIDE_EFFECT : Any, EVENT : Any>(
    initialState: STATE,
) : ViewModel(), KoinComponent {

    private val logger: Logger by inject()

    private val mutableState = MutableStateFlow(initialState)
    private val sideEffects = Channel<SIDE_EFFECT>(Channel.BUFFERED)

    private val screenName = this::class.simpleName.orEmpty().removeSuffix("Model")

    private val failureHandler = CoroutineExceptionHandler { _, throwable ->
        logger.error(tag = LOG_TAG, message = "$screenName failed", throwable = throwable)
    }

    protected val state: STATE
        get() = mutableState.value

    protected val stateChanges: StateFlow<STATE>
        get() = mutableState.asStateFlow()

    abstract fun dispatch(event: EVENT)

    protected abstract fun onFetchData()

    protected fun onFetchDataScope(call: suspend CoroutineScope.(STATE) -> Unit): Job {
        logger.info(tag = LOG_TAG, message = "$screenName opened")
        return screenModelScope(call = call)
    }

    protected fun screenModelScope(call: suspend CoroutineScope.(STATE) -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main.immediate + failureHandler) { call(state) }
    }

    protected fun updateState(reduce: (STATE) -> STATE) {
        mutableState.update(reduce)
    }

    protected suspend fun postSideEffect(effect: SIDE_EFFECT) {
        sideEffects.send(effect)
    }

    @Composable
    fun collectAsState(): State<STATE> = mutableState.collectAsState()

    @Composable
    fun collectSideEffect(onSideEffect: suspend (SIDE_EFFECT) -> Unit) {
        LaunchedEffect(Unit) {
            sideEffects.receiveAsFlow().collect(onSideEffect)
        }
    }
}
