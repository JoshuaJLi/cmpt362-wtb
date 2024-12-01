package ca.wheresthebus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import ca.wheresthebus.R
import ca.wheresthebus.data.ModelFactory
import ca.wheresthebus.data.db.MyMongoDBApp
import ca.wheresthebus.data.model.BusStop
import ca.wheresthebus.data.mongo_model.MongoBusStop
import ca.wheresthebus.utils.TextUtils
import ca.wheresthebus.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import io.realm.kotlin.ext.query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class BusNotifierService : LifecycleService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Cancel all coroutines when service is destroyed
    }

    companion object {
        private const val CHANNEL_ID = "WTB_NOTIF_CHANNEL_ID"
        private const val INTENT_STOP_ID = "STOP_ID"
        private const val LAT_LNG_BUFFERS = 0.005
    }

    private val realm = MyMongoDBApp.realm
    private val modelFactory = ModelFactory()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return super.onStartCommand(intent, flags, startId)
        createNotificationChannel()

        serviceScope.launch {
            if (intent.getStringExtra(INTENT_STOP_ID) != null) {
                val stopId = intent.getStringExtra(INTENT_STOP_ID)!!
                getStopById(stopId)?.let {
                    notifyNextBusses(it)
                }
            } else {
                getNearestStopAndSendNotification()
            }
        }

        return START_NOT_STICKY
    }

    private fun getNearbyStops(latitude: Double, longitude: Double): List<BusStop> {
        val nearbyQuery = "(lat >= $0 AND lat <= $1) AND (lng >= $2 AND lng <= $3)"

        val result = realm.query<MongoBusStop>(
            nearbyQuery,
            latitude - LAT_LNG_BUFFERS,
            latitude + LAT_LNG_BUFFERS,
            longitude - LAT_LNG_BUFFERS,
            longitude + LAT_LNG_BUFFERS
        )

        return result.find().map { modelFactory.toBusStop(it) }
    }

    private fun getStopById(stopId: String): BusStop? {
        val result = realm.query<MongoBusStop>("id == $0", stopId)
        return result.find().map { modelFactory.toBusStop(it) }.firstOrNull()
    }

    // Need to create notification channels for API level 26 and higher
    private fun createNotificationChannel() {
        val name = "Upcoming Bus"
        val descriptionText = "Notifications for upcoming bus times"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun getNearestStopAndSendNotification() {
        if (!Utils.checkLocationPermission(this)) return

        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        serviceScope.launch {
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            )
                .addOnSuccessListener { location ->
                    val closestStop = getClosestStop(location)
                    notifyNextBusses(closestStop)
                }
        }
    }


    private fun getClosestStop(location: Location): BusStop {
        val nearestStop = getNearbyStops(location.latitude, location.longitude)
            .associateBy { location.distanceTo(it.location) }
            .minBy { (location, _) -> location }
            .value

        return nearestStop
    }

    private fun notifyNextBusses(stop: BusStop) = runBlocking {
        serviceScope.launch {
            val busTimes =
                GtfsRealtimeHelper.getBusTimes(stop.routes.map { Pair(stop.id, it.id) })
            val routeTimePair = stop.routes.associateBy {
                busTimes[Pair(stop.id, it.id)]
            }

            val content = routeTimePair.map {
                "${it.value.shortName}: ${TextUtils.upcomingBusesString(it.key)}"
            }.joinToString(separator = "\n")

            val notification = NotificationCompat.Builder(this@BusNotifierService, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nearby_black_dp24)
                .setContentTitle(stop.name)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val notificationManager =
                this@BusNotifierService.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(20, notification)
        }
    }
}