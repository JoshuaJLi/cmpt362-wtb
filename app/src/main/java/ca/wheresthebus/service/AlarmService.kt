package ca.wheresthebus.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ca.wheresthebus.data.model.ScheduledTrip
import java.time.LocalDateTime


class AlarmService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Intent(context, LiveNotificationService::class.java).also {
            it.putExtras(intent)
            it.action = intent.action
            context.startService(it)
        }

    }

    companion object {
        fun scheduleTripNotifications(trips : List<ScheduledTrip>, context: Context) {
            val sharedPreferences = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            var notificationId = sharedPreferences.getInt("notification_id", 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            clearNotifications(alarmManager, notificationId, context)
            notificationId = 0

            trips.forEach {trip ->
                val startIntent = Intent(context, AlarmService::class.java).apply {
                    putStringArrayListExtra(LiveNotificationService.EXTRA_NICKNAMES, trip.stops.map { it.nickname }.toCollection(ArrayList()))
                    putStringArrayListExtra(LiveNotificationService.EXTRA_STOP_IDS, trip.stops.map { it.busStop.id.value }.toCollection(ArrayList()))
                    putStringArrayListExtra(LiveNotificationService.EXTRA_ROUTE_IDS, trip.stops.map { it.route.id.value }.toCollection(ArrayList()))
                    putExtra(LiveNotificationService.EXTRA_DURATION, trip.duration.toMinutes())
                    putExtra(LiveNotificationService.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(LiveNotificationService.EXTRA_TRIP_NICKNAME, trip.nickname)
                }

                val stopIntent = Intent(context, AlarmService::class.java).apply {
                    action = LiveNotificationService.ACTION_STOP
                    putExtra(LiveNotificationService.EXTRA_NOTIFICATION_ID, notificationId)
                }

                val upcomingTimes = trip.activeTimes.map { it.getNextTime(LocalDateTime.now()) }

                upcomingTimes.forEach { time ->
                    val pendingLaunchIntent =  PendingIntent.getBroadcast(
                        context,
                        notificationId,
                        startIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    Log.d("AlarmService", "Alarm set with id $notificationId")
                    notificationId++

                    val pendingStopIntent =  PendingIntent.getBroadcast(
                        context,
                        notificationId,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notificationId++

//                alarmManager.setInexactRepeating(
//                    AlarmManager.RTC_WAKEUP,
//                    time.toEpochSecond(ZoneOffset.of(ZoneId.systemDefault().id)) * 1000,
//                    AlarmManager.INTERVAL_DAY * 7,
//                    pendingIntent
//                    )

                    //                alarmManager.setInexactRepeating(
//                    AlarmManager.RTC_WAKEUP,
//                    time.toEpochSecond(ZoneOffset.of(ZoneId.systemDefault().id)) * 1000,
//                    AlarmManager.INTERVAL_DAY * 7 + (trip.duration.toMillis()),
//                    pendingStopIntent
//                    )

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + (1000 * 5),
                        pendingLaunchIntent
                    )

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + (1000 * 15),
                        pendingStopIntent
                    )
                }
            }
            sharedPreferences.edit().putInt("notification_id", notificationId).apply()
        }

        private fun clearNotifications(alarmManager: AlarmManager, notificationId: Int, context: Context) {
            for (id in 0 until notificationId) {
                val intent = Intent(context, AlarmService::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }    }


}