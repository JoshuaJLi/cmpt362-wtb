package ca.wheresthebus.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ca.wheresthebus.data.IntentRequestCode
import ca.wheresthebus.data.model.ScheduledTrip
import java.time.Duration
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
                val (startIntent, stopIntent) = createStartAndStopIntent(context, trip)

                val upcomingTimes = trip.activeTimes.map { it.getNextTime(LocalDateTime.now()) }

                upcomingTimes.forEach { time ->
                    val (pendingLaunchIntent, pendingStopIntent) = createStartStopPendingIntent(
                        context,
                        trip,
                        startIntent,
                        stopIntent
                    )

                    scheduleService(
                        alarmManager,
                        time,
                        trip.duration,
                        pendingLaunchIntent,
                        pendingStopIntent
                    )
                }
            }
        }

        fun startTripNow(trips: List<ScheduledTrip>, context: Context) {
            Log.d("Alarm", "Starting trip now")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            trips.forEach { trip ->
                val (startIntent, stopIntent) = createStartAndStopIntent(context, trip)
                val (pendStart, pendStop) = createStartStopPendingIntent(
                    context,
                    trip,
                    startIntent,
                    stopIntent
                )
                startService(alarmManager, trip.duration, pendStart, pendStop)
            }
        }

        private fun createStartAndStopIntent(
            context: Context,
            trip: ScheduledTrip
        ): Pair<Intent, Intent> {
            val startIntent = Intent(context, AlarmService::class.java).apply {
                putExtra(LiveNotificationService.EXTRA_TRIP_ID, trip.id.value)
            }

            val stopIntent = Intent(context, AlarmService::class.java).apply {
                action = LiveNotificationService.ACTION_STOP
                putExtra(LiveNotificationService.EXTRA_TRIP_ID, trip.id.value)
            }
            return Pair(startIntent, stopIntent)
        }

        private fun createStartStopPendingIntent(
            context: Context,
            trip: ScheduledTrip,
            startIntent: Intent,
            stopIntent: Intent
        ): Pair<PendingIntent, PendingIntent> {
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
            return Pair(pendingLaunchIntent, pendingStopIntent)
        }

        private fun scheduleService(
            alarmManager: AlarmManager,
            scheduledTime: LocalDateTime,
            duration: Duration,
            pendingLaunchIntent: PendingIntent,
            pendingStopIntent: PendingIntent
        ) {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                scheduledTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                AlarmManager.INTERVAL_DAY * 7,
                pendingLaunchIntent
            )

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                scheduledTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                AlarmManager.INTERVAL_DAY * 7 + (duration.toMillis()),
                pendingStopIntent
            )
        }

        private fun startService(
            alarmManager: AlarmManager,
            duration: Duration,
            pendingLaunchIntent: PendingIntent,
            pendingStopIntent: PendingIntent
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                pendingLaunchIntent
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + duration.toMillis(),
                pendingStopIntent
            )
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