package app.trainer.feature.schedule.domain

import app.trainer.data.schedule.SlotSeriesResult

class SlotSeriesResults {

    private val results = mutableMapOf<String, SlotSeriesResult>()

    fun put(batchId: String, result: SlotSeriesResult) {
        results[batchId] = result
    }

    fun take(batchId: String): SlotSeriesResult? = results.remove(batchId)
}
