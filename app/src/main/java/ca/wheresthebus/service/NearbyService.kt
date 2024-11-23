package ca.wheresthebus.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import ca.wheresthebus.constants.Constants.ACTION_START_SERVICE
import ca.wheresthebus.constants.Constants.ACTION_STOP_SERVICE

import android.content.Intent
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import ca.wheresthebus.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


class NearbyService : LifecycleService() {

    lateinit var locationCallback: LocationCallback;
    lateinit var lastLocation: Location;
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient;



    companion object {
        val isTracking = MutableLiveData<Boolean>();
    }

    override fun onCreate() {
        super.onCreate();

        initializeInitialValues();
        initializeLocationCallback();

        isTracking.observe(this) {
            updateLocation(it);
        };
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                ACTION_START_SERVICE -> {
                    // start service
                    Log.d("NearbyService", "Starting service... tracking location.");
                    startForegroundService();
                }
                ACTION_STOP_SERVICE -> {
                    // stop service
                    Log.d("NearbyService", "Stopping service... no longer tracking location.");
                    stopForegroundService();
                }
                else -> {
                    Log.d("NearbyService", "Unknown action: ${it.action}");
                    stopForegroundService(); // just stop the service in any error / other case
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private fun initializeInitialValues() {
        isTracking.postValue(false);
        lastLocation = Location("")

        // https://stackoverflow.com/questions/78444050/kotlin-android-use-fusedlocationproviderclient-for-requestlocationupdates-for-c
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this); //
    }

    private fun initializeLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult);

                if (isTracking.value == true) {
                    locationResult.locations.let { locations ->
                        for (location in locations) {
                            lastLocation = location
                        }
                    }
                }

                Log.d("NearbyService", "Location update: ${locationResult.lastLocation}");
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocation(isTracking: Boolean) {
        if (isTracking) {
            // https://stackoverflow.com/questions/66489605/is-constructor-locationrequest-deprecated-in-google-maps-v2
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(3000L) // sets the fastest interval
                .setMaxUpdateAgeMillis(5000L) // sets the maximum time that the location is considered up-to-date
                .build();

            fusedLocationProviderClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            );
        }
    }

    private fun startForegroundService() {
        initializeInitialValues();

        isTracking.postValue(true);

        // open up the main activity when this notification is tapped
        // use a lambda to create an intent to open the main activity
        val pendingIntent = PendingIntent.getActivity(this,
            0,
            Intent(
                this,
                MainActivity::class.java
            ),
            PendingIntent.FLAG_IMMUTABLE
        );

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel("NearbyServiceChannel", "NearbyService", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // build a notification to tell the user that their location is currently being tracked
        // as they search for nearby locations
        val notificationBuilder = NotificationCompat.Builder(this, "NearbyServiceChannel")
            .setAutoCancel(false)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Where's The Bus")
            .setContentText("Tracking your location for nearby stops...")
            .setContentIntent(pendingIntent);

        startForeground(1, notificationBuilder.build()); // this service doesn't need a notification, it'll stop when the fragment is closed
    }

    private fun stopForegroundService() {
        isTracking.postValue(false);
        stopForegroundService();
    }

}