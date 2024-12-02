package ca.wheresthebus.service

import ca.wheresthebus.Globals.BUS_RETRIEVAL_MAX
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.UpcomingTime

class GtfsData {
    companion object {

        // Requests GTFS arrival times data from both realtime and static sources
        suspend fun getBusTimes(stopsInfo: List<StopRequest>): MutableMap< StopRequest, List<UpcomingTime>> {
            return combine(GtfsStaticHelper.getBusTimes(stopsInfo), GtfsRealtimeHelper.getBusTimes(stopsInfo))
        }

        // Function to help supplement missing realtime GTFS data with static
        fun combine(
            static: MutableMap<StopRequest, List<UpcomingTime>>,
            realtime: MutableMap<StopRequest, List<UpcomingTime>>
        ): MutableMap<StopRequest, List<UpcomingTime>> {
            val combined = mutableMapOf<StopRequest, List<UpcomingTime>>()

            // Both set of keys guaranteed to be the same
            for (request in realtime.keys) {
                val realtimeData = realtime[request].orEmpty()
                val staticData = static[request].orEmpty()

                val combinedList = when {
                    realtimeData.size >= BUS_RETRIEVAL_MAX -> realtimeData // keep all realtime results
                    realtimeData.isEmpty() -> staticData // no realtime results found, use static
                    else -> realtimeData + staticData.drop(realtimeData.size)
                }
                combined[request] = combinedList
            }
            return combined
        }
    }
}