package ca.wheresthebus.service

import android.icu.util.Calendar
import android.util.Log
import ca.wheresthebus.Globals.BUS_RETRIEVAL_MAX
import ca.wheresthebus.Globals.SERVICE_ID_MON_TO_FRI
import ca.wheresthebus.Globals.SERVICE_ID_SAT
import ca.wheresthebus.Globals.SERVICE_ID_SUN
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.mongo_model.MongoArrivalTime
import ca.wheresthebus.data.mongo_model.MongoStopTime
import io.realm.kotlin.ext.query
import java.time.Duration
import java.time.LocalTime

class GtfsStaticHelper {
    companion object {
        private const val SEARCH_QUERY = "stopId == $0 AND routeId == $1 AND serviceId == $2"
        private val realm = MyMongoDBApp.realm
        private val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        private val currentServiceId = when (currentDayOfWeek) {
            Calendar.SATURDAY -> SERVICE_ID_SAT
            Calendar.SUNDAY -> SERVICE_ID_SUN
            else -> SERVICE_ID_MON_TO_FRI
        }

        fun getBusTimes(stopsInfo: List<Pair<StopId, RouteId>>): MutableMap<StopId, List<Duration>> {
            println("Called getBusTimes static")
            return try {
                stopsInfo.associate { (stopId, routeId) ->
                    stopId to getTimeUntilNextBus(stopId.value.toInt(), routeId.value.toInt())
                }.toMutableMap()
            } catch (e: Exception) {
                Log.e("GTFS", "Error fetching GTFS static data", e)
                mutableMapOf()
            }
        }

        private fun getTimeUntilNextBus(stopId: Int, routeId: Int): List<Duration> {
            return convertBusTimes(getStaticArrivalTime(stopId, routeId))
        }

        // Returns a list of LocalTimes representing the scheduled arrival for a bus
        private fun getStaticArrivalTime(stopId: Int, routeId: Int): List<LocalTime> {
            val arrivalTimes: MutableList<LocalTime> = mutableListOf()

            // Grab potential service overflows from prev serviceId
            when (currentDayOfWeek) {
                Calendar.SATURDAY -> arrivalTimes.addAll(getServiceOverflows(stopId, routeId, SERVICE_ID_MON_TO_FRI))
                Calendar.SUNDAY -> arrivalTimes.addAll(getServiceOverflows(stopId, routeId, SERVICE_ID_SAT))
                Calendar.MONDAY -> arrivalTimes.addAll(getServiceOverflows(stopId, routeId, SERVICE_ID_SUN))
            }

            // Append arrivalTimes from today's serviceId
            realm.query<MongoStopTime>(
                SEARCH_QUERY, stopId, routeId, currentServiceId
            ).find().firstOrNull()?.let { stopTime ->
                arrivalTimes.addAll(stopTime.arrivalTimes.map { arrivalTime ->
                    LocalTime.of((arrivalTime.hour % 24), arrivalTime.minute)
                })
            }

            return arrivalTimes
        }

        // Because there are scheduled busses that actually over flows to the next day
            // Their service runs past midnight -> but will have the previous days serviceId
        private fun getServiceOverflows(stopId: Int, routeId: Int, serviceId: Int): List<LocalTime> {
            val stopTime = realm.query<MongoStopTime>(
                SEARCH_QUERY, stopId, routeId, serviceId
            ).find().firstOrNull()

            val overflowTimes: List<MongoArrivalTime> =
                stopTime?.arrivalTimes?.filter { it.hour > 23 } ?: listOf()

            // convert to valid 24 hour local times -> 24:00 becomes 00:00 etc..
            return overflowTimes.map { LocalTime.of(it.hour - 24, it.minute) }
                .takeIf { overflowTimes.isNotEmpty() } ?: mutableListOf()
        }

        // Return future scheduled times as time until
        private fun convertBusTimes(arrivalTimes: List<LocalTime>): List<Duration> {
            val currentTime = LocalTime.now()
            val futureTimes = arrivalTimes.filter { it.isAfter(currentTime) }

            return futureTimes
                .sorted()
                .take(BUS_RETRIEVAL_MAX)
                .map { Duration.between(currentTime, it) }
        }
    }
}