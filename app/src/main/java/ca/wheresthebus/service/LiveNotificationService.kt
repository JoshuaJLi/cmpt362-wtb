package ca.wheresthebus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import ca.wheresthebus.MainActivity
import ca.wheresthebus.MainDBViewModel
import ca.wheresthebus.R
import ca.wheresthebus.data.ScheduledTripId
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.model.FavouriteStop
import ca.wheresthebus.data.model.ScheduledTrip
import ca.wheresthebus.utils.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.LocalDateTime

class LiveNotificationService : LifecycleService() {
    private val activeIds: MutableMap<ScheduledTripId, Int> = mutableMapOf()

    private val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)

    companion object {
        const val CHANNEL_ID = "live_notification_channel_id"

        const val EXTRA_TRIP_ID = "schedule_trip_ids"

        const val ACTION_NAVIGATE_TO_TRIP = "navigate_to_trip"
        const val ACTION_STOP = "action_stop"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val tripId = it.getStringExtra(EXTRA_TRIP_ID) ?: return STOP_FOREGROUND_REMOVE
            val trip = MainDBViewModel.getTripById(ScheduledTripId(tripId)) ?: return STOP_FOREGROUND_REMOVE

            if (intent.action == ACTION_STOP) {
                cancelForegroundService(trip)
                return STOP_FOREGROUND_REMOVE
            }


            startBusPolling(trip)
            return START_STICKY
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun cancelForegroundService(trip: ScheduledTrip) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        activeIds[trip.id]?.let {
            notificationManager.cancel(it)
            activeIds.remove(trip.id)
            Log.d("Notification", "Killed $it, remaining $activeIds")
        }

        if (activeIds.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startBusPolling(trip : ScheduledTrip) {
        createActiveNotification(trip)

        lifecycleScope.launch(Dispatchers.IO) {
            pollBuses(trip).collect { result ->
                updateNotification(trip, trip.stops.associateWith {
                    result[Pair(it.busStop.id, it.route.id)]
                })
            }
        }
    }


    private fun pollBuses(trip: ScheduledTrip): Flow<Map<StopRequest, List<Duration>>> =
        flow {
            while (activeIds.contains(trip.id)) {
                emit(GtfsRealtimeHelper.getBusTimes(trip.stops.map { Pair(it.busStop.id, it.route.id) }))

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

    private fun createActiveNotification(trip: ScheduledTrip) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(CHANNEL_ID, "Trips", NotificationManager.IMPORTANCE_HIGH).let {
            it.description = "Live trip notifications"
            notificationManager.createNotificationChannel(it)
        }

        activeIds[trip.id] = System.currentTimeMillis().toInt()

        // notification for telling the user the app is running
        val notification = getBasicNotification(trip)
            .setContentTitle(trip.nickname)
            .build()

        startForeground(activeIds[trip.id]!!, notification)
    }

    private fun updateNotification(trip: ScheduledTrip, stopTimes: Map<FavouriteStop, List<Duration>?>) {
        val content = stopTimes.map {
            "${it.key.nickname.ifEmpty { it.key.route.shortName }}: ${TextUtils.upcomingBusesString(it.value)}"
        }.joinToString(separator = "\n")

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = getBasicNotification(trip)
            .setContentTitle(trip.nickname)
            .setContentText(content)

        notificationManager.notify(activeIds[trip.id]!!, notification.build())
    }

    private fun getBasicNotification(trip : ScheduledTrip): NotificationCompat.Builder {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_TRIP_ID, trip.id.value)
        }

        val killService =  PendingIntent.getBroadcast(
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
            .addAction(0, "Stop", killService)
    }
}