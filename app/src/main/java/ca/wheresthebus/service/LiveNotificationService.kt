package ca.wheresthebus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import ca.wheresthebus.MainActivity
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.data.IntentRequestCode
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.ScheduledTripId
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.StopId
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.utils.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration

class LiveNotificationService : LifecycleService() {
    private val activeIds: MutableSet<ScheduledTripId> = mutableSetOf()

    private val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)

    companion object {
        const val CHANNEL_ID = "live_notification_channel_id"

        const val EXTRA_TRIP_ID = "schedule_trip_ids"

        const val ACTION_NAVIGATE_TO_TRIP = "navigate_to_trip"
        const val ACTION_STOP = "action_stop"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val tripId = it.getStringExtra(EXTRA_TRIP_ID) ?: return START_STICKY
            val trip = MainDBViewModel.getTripById(ScheduledTripId(tripId)) ?: return START_STICKY

            if (intent.action == ACTION_STOP) {
                cancelForegroundService(trip)
                return START_NOT_STICKY
            }


            startBusPolling(trip)
            return START_STICKY
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun cancelForegroundService(trip: ScheduledTrip) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(trip.requestCode.value)
        activeIds.remove(trip.id)


        if (activeIds.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startBusPolling(trip : ScheduledTrip) {
        createActiveNotification(trip)

        lifecycleScope.launch(Dispatchers.IO) {
            pollBuses(trip.stops).collect { result ->
                updateNotification(trip, trip.stops.associateWith {
                    result[Pair(it.busStop.id, it.route.id)]
                })
            }
        }
    }


    private fun pollBuses(watches: ArrayList<FavouriteStop>): Flow<Map<StopRequest, List<Duration>>> =
        flow {
            while (true) {
                emit(GtfsRealtimeHelper.getBusTimes(watches.map { Pair(it.busStop.id, it.route.id) }))

                val minutes = determineDelay()
                delay(Duration.ofMinutes(minutes))
            }
        }

    private fun determineDelay() : Long {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)

        val pollTime = sharedPreferences.getString(getString(R.string.key_trip_poll_time), "1")

        return  pollTime?.toLong() ?: 1L
    }

    private fun createActiveNotification(
        trip: ScheduledTrip,
    ) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(CHANNEL_ID, "Trips", NotificationManager.IMPORTANCE_HIGH).let {
            it.description = "Live trip notifications"
            notificationManager.createNotificationChannel(it)
        }

        activeIds.add(trip.id)

        // notification for telling the user the app is running
        val notification = getBasicNotification(trip)
            .setContentTitle(trip.nickname)
            .build()

        startForeground(trip.requestCode.value, notification)
    }

    private fun updateNotification(trip: ScheduledTrip, stopTimes: Map<FavouriteStop, List<Duration>?>) {
        val content = stopTimes.map {
            "${it.key.nickname}: ${TextUtils.upcomingBusesString(it.value)}"
        }.joinToString(separator = "\n")

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = getBasicNotification(trip)
            .setContentTitle(trip.nickname)
            .setContentText(content)

        notificationManager.notify(trip.requestCode.value, notification.build())
    }

    private fun getBasicNotification(trip : ScheduledTrip): NotificationCompat.Builder {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_TRIP_ID, trip.id.value)
        }
        val pendingStopIntent =  PendingIntent.getBroadcast(
            this,
            trip.requestCode.value,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        return builder
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.baseline_directions_bus_24)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        action = ACTION_NAVIGATE_TO_TRIP
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .clearActions()
            .addAction(0, "Stop", pendingStopIntent)
    }
}