package ca.wheresthebus.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import ca.wheresthebus.R
import ca.wheresthebus.data.model.Schedule


class AlarmService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmService", "Alarm received")

        Intent(context, LiveNotificationService::class.java).also {
            it.putExtras(intent)
            context.startService(it)
        }

    }
}