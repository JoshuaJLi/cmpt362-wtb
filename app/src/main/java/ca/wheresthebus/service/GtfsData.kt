package ca.wheresthebus.service

import ca.wheresthebus.Globals.BUS_RETRIEVAL_MAX
import ca.wheresthebus.data.StopId
import java.time.Duration

class GtfsData {
    companion object {

        // Function to help supplement missing realtime GTFS data with static
        fun combine(
            static: MutableMap<StopId, List<Duration>>,
            realtime: MutableMap<StopId, List<Duration>>
        ): MutableMap<StopId, List<Duration>> {
            val combined = mutableMapOf<StopId, List<Duration>>()

            // Both set of keys guaranteed to be the same
            for (stopId in realtime.keys) {
                val realtimeData = realtime[stopId].orEmpty()
                val staticData = static[stopId].orEmpty()

                val combinedList = when {
                    realtimeData.size >= BUS_RETRIEVAL_MAX -> realtimeData // keep all realtime results
                    realtimeData.isEmpty() -> staticData // no realtime results found, use static
                    else -> realtimeData + staticData.drop(realtimeData.size)
                }
                combined[stopId] = combinedList
            }
            return combined
        }
    }
}