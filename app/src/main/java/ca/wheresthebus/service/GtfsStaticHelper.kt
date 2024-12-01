package ca.wheresthebus.service

import android.icu.util.Calendar
import android.util.Log
import ca.wheresthebus.Globals
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.mongo_model.MongoArrivalTime
import ca.wheresthebus.data.mongo_model.MongoStopTime
import io.realm.kotlin.ext.query
import java.time.Duration

class GtfsStaticHelper {
    companion object {
        private const val SEARCH_QUERY = "stopId == $0 AND routeId == $1 AND serviceId == $2"
        private val realm = MyMongoDBApp.realm
        private val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        private val currentServiceId = when (currentDayOfWeek) {
            Calendar.SATURDAY -> Globals.SERVICE_ID_SAT
            Calendar.SUNDAY -> Globals.SERVICE_ID_SUN
            else -> Globals.SERVICE_ID_MON_TO_FRI
        }

        fun getBusTimes(stopsInfo: List<Pair<StopId, RouteId>>): MutableMap<StopId, List<Duration>> {
            println("Called getBusTimes static")
            return try {
                stopsInfo.associate { (stopId, routeId) ->
                    stopId to getStaticArrivalTime(stopId.value.toInt(), routeId.value.toInt())
                }.toMutableMap()
            } catch (e: Exception) {
                Log.e("GTFS", "Error fetching GTFS static data", e)
                mutableMapOf()
            }
        }

        // Returns a list of durations until the next N arrival times for a bus
        // Identified by the stopId and routeId
        private fun getStaticArrivalTime(stopId: Int, routeId: Int): List<Duration> {
            println("Grabbing times for stopId: $stopId routeId: $routeId")

            val arrivalTimes: MutableList<MongoArrivalTime> = mutableListOf()

            // Grab potential service overflows from prev serviceId
            when (currentDayOfWeek) {
                Calendar.SATURDAY -> {
                    arrivalTimes.addAll(
                        getServiceOverflows(
                            stopId, routeId, Globals.SERVICE_ID_MON_TO_FRI
                        )
                    )
                }

                Calendar.SUNDAY -> {
                    arrivalTimes.addAll(
                        getServiceOverflows(
                            stopId, routeId, Globals.SERVICE_ID_SAT
                        )
                    )
                }

                Calendar.MONDAY -> {
                    arrivalTimes.addAll(
                        getServiceOverflows(
                            stopId, routeId, Globals.SERVICE_ID_SUN
                        )
                    )
                }
            }

            // Append arrivalTimes from today's serviceId
            realm.query<MongoStopTime>(
                SEARCH_QUERY, stopId, routeId, currentServiceId
            ).find().firstOrNull()?.let {
                arrivalTimes.addAll(
                    it.arrivalTimes
                )
            }

            arrivalTimes.forEach {
                println("${it.hour}:${it.minute}")
            }

            // Filter based on current time

            // Convert to durations until in minutes

            // NOTE: DEBUG prints
//            val searchResults = realm.query<MongoStopTime>(
//                SEARCH_QUERY, stopId, routeId, currentServiceId
//            ).find().firstOrNull()
//
//            searchResults?.let { it ->
//                println("Results for stopId: ${it.stopId} routeId: ${it.routeId}")
//                it.arrivalTimes.forEach {
//                    println("${it.hour}:${it.minute}")
//                }
//            }

            // returns a list of durations from current time
            return listOf()
        }

        // Because there scheduled busses that actually over flows to the next day
        // Their service runs past midnight but will have the previous days serviceId
        private fun getServiceOverflows(stopId: Int, routeId: Int, serviceId: Int): List<MongoArrivalTime> {
            // grab all arrival times if the hour > 23
            val stopTime = realm.query<MongoStopTime>(
                SEARCH_QUERY, stopId, routeId, serviceId
            ).find().firstOrNull()

            // convert to valid 24 hour time for current day
            // 24:00 becomes 00:00 etc..
            val overflowTimes: List<MongoArrivalTime> =
                stopTime?.arrivalTimes?.filter { it.hour > 23 } ?: listOf()

            if (overflowTimes.isNotEmpty()) {
//                overflowTimes.map { it.hour -= 24 } // this modifies the actual obj in the database???
                return overflowTimes
            } else {
                return listOf()
            }
        }
    }
}