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
        readStops()

        // Create BusStop object

        // Add BusStop object to database
        // addToDb(busStop)
    }

    private fun addToDb(busStop : BusStop) {

    }

    private fun readCalendar() {
        
    }

    private fun readStops() {
        val inputStream : InputStream = context.assets.open("stop_times.txt")
        val reader : Reader = BufferedReader(InputStreamReader(inputStream))

        val timeTaken = measureTimeMillis {
            reader.useLines { lines ->
                for (line in lines) {
                    val firstColumn = line.split(",")[0]  // Assuming CSV format
//                    Log.d("MyTag", "First column value: $firstColumn")
                    // Process the first column value if needed, e.g., store it in a list or perform calculations
                }
            }
        }

        val location = Location("dummy")
        location.latitude = 3.0
        location.longitude = 6.0

        val busStop = BusStop(StopCode("4"), StopId("5"), "test", location, listOf())

        Log.d("MyTag", "Time taken to process the file: $timeTaken ms")
    }

    private fun sortFile(fileName : String, fieldToSort : String) : Boolean {
        
        return true
    }

    private fun readStopTimes() {

    }

    private fun readTrips() {

    }
}