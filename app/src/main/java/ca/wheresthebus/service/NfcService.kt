package ca.wheresthebus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ca.wheresthebus.R

class NfcService {
    companion object {

        // Need to create notification channels for API level 26 and higher
        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "NFC Notifications"
                val descriptionText = "Notifications for NFC tag detection"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel("WTB_NOTIF_CHANNEL_ID", name, importance).apply {
                    description = descriptionText
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun sendNfcNotification(context: Context, title: String, content: String) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationIntent = Intent(context, NfcService::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(context, "WTB_NOTIF_CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_nearby_black_dp24) // Using some random icon here for now
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(0, notification)
        }

        fun handleTap(context: Context) {
            createNotificationChannel(context)

            // Do some work here to grab nearest bus or some data
            // Imagine some logic here to create live notification
            sendNfcNotification(context, "Imagine this is a live notification title", "Some interesting notification content.")
        }
    }
}