package ca.wheresthebus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import ca.wheresthebus.MainActivity
import ca.wheresthebus.R
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopId
import kotlinx.coroutines.flow.flow
import java.time.Duration

class LiveNotificationService : LifecycleService() {
    private val watches : MutableList<StopWatches> = mutableListOf()

    data class StopWatches(
        val notificationId : Int,
        val nickname : String,
        val stopId: StopId,
        val routeId: RouteId,
        val duration : Duration
    )


    companion object {
        const val CHANNEL_ID = "live_notification_channel_id"
        const val NOTIFICATION_ID = 1

        const val EXTRA_NICKNAMES = "bus_nicknames"
        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_STOP_IDS = "stop_id"
        const val EXTRA_ROUTE_IDS = "route_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TRIP_NICKNAME = "trip_nickname"

        const val ACTION_NAVIGATE_TO_TRIP = "navigate_to_trip"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val nicknames = it.getStringArrayListExtra(EXTRA_NICKNAMES)
            val duration = it.getLongExtra(EXTRA_DURATION, 60)
            val stopIds = it.getStringArrayListExtra(EXTRA_STOP_IDS)
            val routeIds = it.getStringArrayListExtra(EXTRA_ROUTE_IDS)
            val notificationId = it.getIntExtra(EXTRA_NOTIFICATION_ID, 1)
            val tripNickname = it.getStringExtra(EXTRA_TRIP_NICKNAME)

            if (nicknames == null || stopIds == null || routeIds == null || tripNickname == null) {
                return super.onStartCommand(intent, flags, startId)
            }

            val watches = stopIds
                .zip(routeIds)
                .zip(nicknames)
                { (stopId, routeId), nickname -> StopWatches(0, nickname, StopId(stopId), RouteId(routeId), Duration.ofMinutes(duration)) }
                .toList()

            Log.d("LiveNotificationService", "Starting notification service")

            startBusPolling(tripNickname, notificationId, watches)

        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startBusPolling(nickname: String, notificationId: Int, watches: List<StopWatches>
    ) {
        createActiveNotification(nickname, notificationId, watches)


        val flow = flow<List<Duration>> {
            while (true) {

            }
        }
    }

    private fun createActiveNotification(
        nickname: String,
        notificationId: Int,
        watches: List<StopWatches>
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(CHANNEL_ID, "Trips", NotificationManager.IMPORTANCE_DEFAULT).let {
            it.description = "Live trip notifications"
            notificationManager.createNotificationChannel(it)
        }


        // notification for telling the user the app is running
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.baseline_directions_bus_24)
            .setContentTitle(nickname)
            .setContentText("Here is the body of the text")
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    // set flag so it opens the activity instead of starting a new one
                    Intent(this, MainActivity::class.java).apply {
                        action = ACTION_NAVIGATE_TO_TRIP
                    },
                    FLAG_IMMUTABLE
                )
            )
            .build()
        Log.d("LiveNotificationService", "Starting notification with information: $nickname, $notificationId, $watches")

        startForeground(notificationId, notif)
    }
}