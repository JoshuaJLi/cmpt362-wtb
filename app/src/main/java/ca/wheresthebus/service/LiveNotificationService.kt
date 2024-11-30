package ca.wheresthebus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import ca.wheresthebus.R
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopId
import kotlinx.coroutines.flow.flow
import java.time.Duration

class LiveNotificationService : LifecycleService() {
    private val watches : MutableList<StopWatches> = mutableListOf()

    data class StopWatches(
        val nickname : String,
        val stopId: StopId,
        val routeId: RouteId
    )


    companion object {
        const val CHANNEL_ID = "live_notification_channel_id"
        const val NOTIFICATION_ID = 1

        const val EXTRA_NICKNAMES = "bus_nicknames"
        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_STOP_IDS = "stop_id"
        const val EXTRA_ROUTE_IDS = "route_id"
    }

    override fun onCreate() {
        super.onCreate()


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LiveNotificationService", "Starting notification service")
        createActiveNotification()

        startBusPolling()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startBusPolling() {
        val flow = flow<List<Duration>> {
            while (true) {

            }
        }
    }

    private fun createActiveNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(CHANNEL_ID, "Trips", NotificationManager.IMPORTANCE_DEFAULT).let {
            it.description = "Channel for live trip notifications"
            notificationManager.createNotificationChannel(it)
        }


        // notification for telling the user the app is running
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.baseline_directions_bus_24)
            .setContentTitle("Here is the title of the notification")
            .setContentText("Here is the body of the text")
//            .setContentIntent(
//                PendingIntent.getActivity(
//                    this,
//                    0,
//                    // set flag so it opens the activity instead of starting a new one
//                    Intent(this, GpsActivity::class.java).apply {
//                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
//                    },
//                    FLAG_IMMUTABLE
//                )
//            )
            .build()

        startForeground(NOTIFICATION_ID, notif)
    }
}