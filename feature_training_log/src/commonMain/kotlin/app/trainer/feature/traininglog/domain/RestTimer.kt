package app.trainer.feature.traininglog.domain

import app.trainer.data.push.RestTimerAlarm
import app.trainer.data.traininglog.RestIntervalStore
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

const val REST_STEP_SECONDS = 15

private const val MAX_REST_SECONDS = 600
private const val TICK_DELAY_MS = 1000L
private const val MILLIS_IN_SECOND = 1000

data class RestCountdown(val remainingSeconds: Int, val totalSeconds: Int)

private data class RunningRest(
    val exerciseId: String,
    val endsAtMillis: Long,
    val totalSeconds: Int,
) {

    fun remainingSecondsAt(nowMillis: Long): Int {
        val left = endsAtMillis - nowMillis
        if (left <= 0) return 0
        return ((left + MILLIS_IN_SECOND - 1) / MILLIS_IN_SECOND).toInt()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimer(
    private val alarm: RestTimerAlarm,
    private val restIntervalStore: RestIntervalStore,
) {

    private val running = MutableStateFlow<RunningRest?>(null)

    val countdown: Flow<RestCountdown?> = running.flatMapLatest { rest ->
        if (rest == null) flowOf(null) else ticking(rest)
    }

    suspend fun start(exerciseId: String, plannedRestSeconds: Int?) {
        val seconds = plannedRestSeconds
            ?: restIntervalStore.secondsFor(exerciseId)
            ?: restIntervalStore.defaultSeconds()
        run(exerciseId = exerciseId, seconds = seconds)
    }

    suspend fun extend() {
        val rest = running.value ?: return
        val seconds = (rest.totalSeconds + REST_STEP_SECONDS).coerceAtMost(MAX_REST_SECONDS)
        if (seconds == rest.totalSeconds) return
        restIntervalStore.remember(exerciseId = rest.exerciseId, seconds = seconds)
        run(exerciseId = rest.exerciseId, seconds = seconds)
    }

    fun stop() {
        alarm.cancel()
        running.value = null
    }

    private fun run(exerciseId: String, seconds: Int) {
        alarm.schedule(seconds)
        running.value = RunningRest(
            exerciseId = exerciseId,
            endsAtMillis = nowMillis() + seconds.toLong() * MILLIS_IN_SECOND,
            totalSeconds = seconds,
        )
    }

    private fun ticking(rest: RunningRest): Flow<RestCountdown?> = flow {
        var remaining = rest.remainingSecondsAt(nowMillis())
        while (remaining > 0) {
            emit(RestCountdown(remainingSeconds = remaining, totalSeconds = rest.totalSeconds))
            delay(TICK_DELAY_MS)
            remaining = rest.remainingSecondsAt(nowMillis())
        }
        emit(null)
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
