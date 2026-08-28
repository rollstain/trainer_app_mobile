package app.trainer.data.traininglog

import app.trainer.entities.LocalDataCleaner

interface RestIntervalStore : LocalDataCleaner {

    suspend fun defaultSeconds(): Int

    suspend fun rememberDefault(seconds: Int)

    suspend fun secondsFor(exerciseId: String): Int?

    suspend fun remember(exerciseId: String, seconds: Int)
}
