package ca.wheresthebus.ui.nearby

import android.content.Context
import android.location.LocationRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ca.wheresthebus.data.model.Stop
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import java.io.BufferedReader
import java.io.InputStreamReader

class NearbyViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    lateinit var context: Context;

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient;

    var stopList: ArrayList<Stop> = ArrayList<Stop>();

    fun intializeContext(context: Context) {
        this.context = context
    }

    fun initializeFusedLocationProviderClient(fusedLocationProviderClient: FusedLocationProviderClient) {
        this.fusedLocationProviderClient = fusedLocationProviderClient
    }

    fun loadStopsFromCSV() {
        val minput = InputStreamReader(context?.assets?.open("stops.csv"))
        val reader = BufferedReader(minput)

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val row: List<String> = line!!.split(",")
            try {
                val stopId = row[1]
                val stopName = row[2]
                val stopLat = row[4].toDouble()
                val stopLon = row[5].toDouble()

                val newStop = Stop(stopId, stopName, stopLat, stopLon)
                stopList.add(newStop)
            } catch (e: NumberFormatException) {
                println("Failed to parse row: ${line}. Error: ${e.message}")
            } catch (e: IndexOutOfBoundsException) {
                println("Row has insufficient columns: $line")
            }
        }
    }
}