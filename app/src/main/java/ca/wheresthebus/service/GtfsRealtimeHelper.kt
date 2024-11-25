package ca.wheresthebus.service

import android.util.Log
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopId
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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

        suspend fun getBusTimes(stopId: StopId, routeId: RouteId, amountOfTimes: Int): List<LocalDateTime> {
            return try {
                val feedMessage = callGtfsRealtime()
                val busTimes = grabBusTimes(feedMessage, stopId, routeId)
                filterBusTimes(busTimes, amountOfTimes)
            } catch (e: Exception) {
                Log.e("GTFS", "Error fetching GTFS realtime data", e)
                emptyList()
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
            val busTimes = mutableListOf<Long>()

            // Iterate through the entities in the feed message and add matching bus times to the list
            feedMessage.entityList.forEach { entity ->
                if (entity.hasTripUpdate()) {
                    val tripUpdate = entity.tripUpdate
                    if (tripUpdate.trip.routeId == routeId.id) {
                        tripUpdate.stopTimeUpdateList.forEach { stopTimeUpdate ->
                            if (stopTimeUpdate.stopId == stopId.id) {
                                stopTimeUpdate.arrival?.time?.let {
                                    busTimes.add(it)
                                }
                            }
                        }
                    }
                }
            }

            return busTimes
        }

        private fun filterBusTimes(busTimes: List<Long>, amountOfTimes: Int): List<LocalDateTime> {
            return busTimes
                .sorted()
                .take(amountOfTimes)
                .map { time ->
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault())
                }
        }
    }
}