package ca.wheresthebus.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import ca.wheresthebus.R
class LiveNotificationService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "live_notification_channel_id"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createActiveNotification()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createActiveNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        createNotificationChannel(notificationManager)


        // notification for telling the user the app is running
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
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

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val name = "notif channel"
        val desc = "a desc of the channel"
        val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
        val channel = android.app.NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = desc

        notificationManager.createNotificationChannel(channel)

    }
}