package ca.wheresthebus.service

import android.util.Log
import ca.wheresthebus.Globals.BUS_RETRIEVAL_MAX
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.UpcomingTime
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.Duration

/**
 *  Class to grab bus times from the GTFS realtime API.
 *
 *  Usage
 *  lifecycleScope.launch {
 *      times = GtfsRealtime.getBusTimes(StopId("70"), RouteId("6612"), 3)
 *  }
 */
class GtfsRealtimeHelper {
    companion object {
        private val client = OkHttpClient()
        private const val GTFS_API_URL = "https://gtfsapi.translink.ca/v3/gtfsrealtime?apikey=${ca.wheresthebus.BuildConfig.GTFS_KEY}"

        suspend fun getBusTimes(stopsInfo: List<StopRequest>): MutableMap< StopRequest, List<UpcomingTime>> {
            return try {
                val feedMessage = callGtfsRealtime()
                stopsInfo.associate { (stopId, routeId) ->
                    (stopId to routeId) to convertBusTimes(grabBusTimes(feedMessage, stopId, routeId))
                }.toMutableMap()
            } catch (e: Exception) {
                Log.e("GTFS", "Error fetching GTFS realtime data", e)
                mutableMapOf()
            }
        }

        /**
         * Calls the GTFS realtime API and returns the response as a FeedMessage
         */
        private suspend fun callGtfsRealtime(): FeedMessage = withContext(Dispatchers.IO) {
            // Create an HTTP request to the GTFS realtime API and get the response
            val request = Request.Builder().url(GTFS_API_URL).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP error: ${response.code} - ${response.message}")
            }

            // Parse the response body as a FeedMessage
            val responseBody = response.body ?: throw Exception("Response body is null")
            FeedMessage.parseFrom(responseBody.byteStream())
        }

        /**
         * Given a feed, provides a list of bus times of size {amountOfTimes}
         * matching the {stopId} and {routeId} parameters.
         */
        private fun grabBusTimes(feedMessage: FeedMessage, stopId: StopId, routeId: RouteId): List<Long> {
            // Iterate through the entities in the feed message and add matching bus times to the list
            return feedMessage.entityList
                .asSequence()
                .filter { entity ->  entity.hasTripUpdate()}
                .map { trip -> trip.tripUpdate }
                .filter { update -> update.trip.routeId == routeId.value}
                .map { update -> update.stopTimeUpdateList }
                .flatten()
                .filter { update -> update.stopId == stopId.value  }
                .map { stopTime -> stopTime.arrival?.time }
                .filterNotNull()
                .toList()
        }

        private fun convertBusTimes(busTimes: List<Long>): List<UpcomingTime> {
            return busTimes.sorted().take(BUS_RETRIEVAL_MAX).map {
                UpcomingTime(
                    true, Duration.between(
                        Instant.now(), Instant.ofEpochSecond(it)
                    )
                )
            }
        }
    }
}