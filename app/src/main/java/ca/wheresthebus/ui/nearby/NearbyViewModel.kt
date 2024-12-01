package ca.wheresthebus.ui.nearby

import android.Manifest
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
import ca.wheresthebus.Globals.LOCATION_UPDATE_MAXIMUM_AGE
import ca.wheresthebus.Globals.LOCATION_UPDATE_MINIMUM_INTERVAL
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.data.model.BusStop
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NearbyViewModel : ViewModel() {

    private lateinit var mainDBViewModel: MainDBViewModel

    // -- properties
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private val _locationUpdates: MutableLiveData<Location> = MutableLiveData<Location>()
    val locationUpdates: LiveData<Location> = _locationUpdates // this makes it so location live data is read only
    val isTracking: MutableLiveData<Boolean> = MutableLiveData<Boolean>(true)

    var busStopList: ArrayList<BusStop> = ArrayList()

    private var isLocationUpdatesRunning = false

    // -- methods
    fun loadStopsFromDatabase() {
        // do this in a coroutine as there's a lot of stops
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                busStopList = mainDBViewModel.getAllStops()?.let { ArrayList(it) }!!
            }
        }
    }

    // all calculations will be done in meters
    // https://stackoverflow.com/questions/43080343/calculate-distance-between-two-locations-in-metre
    fun isInRange(userLocation: LatLng, stopLocation: LatLng, distanceThreshold: Double): Boolean {
        var inRange = false

        try {
            val distance = FloatArray(2)

            Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                stopLocation.latitude,
                stopLocation.longitude,
                distance
            )

            if (distance[0] < distanceThreshold) {
                inRange = true
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }

        return inRange
    }

    fun setMainDBViewModel(mainDBViewModel: MainDBViewModel) {
        this.mainDBViewModel = mainDBViewModel
    }

    fun startLocationUpdates(context: Context) {
        if (isLocationUpdatesRunning) {
            Log.d("NearbyViewModel", "Location updates already running")
            return
        }

        isLocationUpdatesRunning = true

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    LOCATION_UPDATE_MINIMUM_INTERVAL
                ).setMinUpdateIntervalMillis(LOCATION_UPDATE_MINIMUM_INTERVAL)
                    .setMaxUpdateAgeMillis(LOCATION_UPDATE_MAXIMUM_AGE)
                    .build()

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        for (location in locationResult.locations) {
                            _locationUpdates.postValue(location)
                        }
                    }
                }

                if (
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }
        }

        isTracking.postValue(true)

        Log.d("NearbyViewModel", "Location updates started")
    }

    fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)

        isTracking.postValue(false)
        isLocationUpdatesRunning = false

        Log.d("NearbyViewModel", "Location updates stopped")
    }
}