package ca.wheresthebus.ui.nearby

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.wheresthebus.data.model.Stop
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class NearbyViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    // -- properties
    lateinit var lastLocation: Location;
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient;
    lateinit var locationCallback: LocationCallback;
    lateinit var locationRequest: LocationRequest;

    private val _locationUpdates: MutableLiveData<Location> = MutableLiveData<Location>();
    val locationUpdates: LiveData<Location> = _locationUpdates; // this makes it so location live data is read only

    var stopList: ArrayList<Stop> = ArrayList<Stop>();

    // -- methods
    fun loadStopsFromCSV(context: Context) {
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

    fun getLocationPermissions(context: Context) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)

            return
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000L
                ).setMinUpdateIntervalMillis(3000L)
                    .setMaxUpdateAgeMillis(5000L)
                    .build();

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        for (location in locationResult.locations) {
                            _locationUpdates.postValue(location)
                        }
                    }
                };

                // TODO: check for permissions, suppressed for now
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                );
            }
        };

        Log.d("NearbyViewModel", "Location updates started");
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.d("NearbyViewModel", "Location updates stopped");
    }
}