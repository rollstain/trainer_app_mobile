package app.trainer.data.push

interface RestTimerAlarm {

    fun schedule(afterSeconds: Int)

    fun cancel()
}
