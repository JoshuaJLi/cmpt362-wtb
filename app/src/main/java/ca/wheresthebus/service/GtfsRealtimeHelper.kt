package ca.wheresthebus.service

import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopId

class GtfsRealtime {
    fun getBusTimes(stopId: StopId, routeId: RouteId, amountOfTimes: Int) {
        val gtfsRealtimeFeedMessage = callGtfsRealtime()
        val parseFeedMessage = parseGtfsRealtime()

    }

    private fun callGtfsRealtime() {

    }

    private fun parseGtfsRealtime() {

    }
}