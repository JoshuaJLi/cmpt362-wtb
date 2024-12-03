package ca.wheresthebus.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.wheresthebus.data.IntentRequestCode
import ca.wheresthebus.data.model.ScheduledTrip
import java.time.LocalDateTime
import java.time.ZoneId


class AlarmService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Intent(context, LiveNotificationService::class.java).also {
            it.putExtras(intent)
            it.action = intent.action
            context.startService(it)
        }

    }

    companion object {
        fun scheduleTripNotifications(trips: List<ScheduledTrip>, context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            clearNotifications(alarmManager, trips.map { it.requestCode }.toTypedArray(), context)

            trips.forEach { trip ->
                val startIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(LiveNotificationService.EXTRA_TRIP_ID, trip.id.value)
                }

                val stopIntent = Intent(context, AlarmService::class.java).apply {
                    action = LiveNotificationService.ACTION_STOP
                    putExtra(LiveNotificationService.EXTRA_TRIP_ID, trip.id.value)
                }

                val upcomingTimes = trip.activeTimes.map { it.getNextTime(LocalDateTime.now()) }

                upcomingTimes.forEach { time ->
                    val pendingLaunchIntent = PendingIntent.getBroadcast(
                        context,
                        trip.requestCode.value,
                        startIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val pendingStopIntent = PendingIntent.getBroadcast(
                        context,
                        trip.requestCode.value,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingLaunchIntent
                    )

                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        time.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                        AlarmManager.INTERVAL_DAY * 7 + (trip.duration.toMillis()),
                        pendingStopIntent
                    )

//                    alarmManager.setExactAndAllowWhileIdle(
//                        AlarmManager.RTC_WAKEUP,
//                        System.currentTimeMillis() + (1000 * 5),
//                        pendingLaunchIntent
//                    )
//
//                    alarmManager.setExactAndAllowWhileIdle(
//                        AlarmManager.RTC_WAKEUP,
//                        System.currentTimeMillis() + (1000 * 15),
//                        pendingStopIntent
//                    )
                }
            }
        }

        private fun clearNotifications(
            alarmManager: AlarmManager,
            requestCodes: Array<IntentRequestCode>,
            context: Context
        ) {
            requestCodes.forEach {
                val intent = Intent(context, AlarmService::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    it.value,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }

        }

    }
}