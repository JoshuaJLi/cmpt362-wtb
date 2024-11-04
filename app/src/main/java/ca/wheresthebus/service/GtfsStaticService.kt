package ca.wheresthebus.service

import android.content.Context
import android.location.Location
import android.util.Log
import ca.wheresthebus.data.BusStop
import ca.wheresthebus.data.StopCode
import ca.wheresthebus.data.StopId
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import kotlin.system.measureTimeMillis

class GtfsStaticService(private val context : Context) {
    fun grabNextThreeStopTimes(id : StopId) {

    }

    fun parseStaticDataToDb() {
        // add stops into BusStop hashmap
//        val stopsMap : MutableMap<String, BusStop.Builder> = getStopsAsHashmap()

        // add schedules from stop_times.txt into hashmap
//        addSchedulesToHashmap(stopsMap)
        // add hashmap values into db
//        addStopsToDb()
    }

    private fun addStopsToDb() {

    }

    private fun getStopsAsHashmap() {
//        val stopsMap = mutableMapOf<String, BusStop.Builder>()
//
//        val inputStream : InputStream = context.assets.open("stops.txt")
//        val reader : Reader = BufferedReader(InputStreamReader(inputStream))
//
//        reader.useLines { lines ->
//            lines.forEach { line ->
//                val fields = line.split(",")
//
//                val stopId = fields[0]
//                val stopCode = fields[1]
//                val stopName = fields[2]
//                val stopLat = fields[4].toDouble()
//                val stopLon = fields[5].toDouble()
//                val location : Location = Location("stop").apply {
//                    latitude = stopLat
//                    longitude = stopLon
//                }
//
//                val builder = BusStop.Builder()
//                    .setCode(StopCode(stopCode))
//                    .setId(StopId(stopId))
//                    .setName(stopName)
//                    .setLocation(location)
//
//                stopsMap[stopId] = builder
//            }
//        }
//        return stopsMap
    }

    private fun addSchedulesToHashmap() {

    }
}