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
import ca.wheresthebus.R
import ca.wheresthebus.data.RouteId
import ca.wheresthebus.data.StopRequest
import ca.wheresthebus.data.StopId
import ca.wheresthebus.utils.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import kotlin.math.min

class LiveNotificationService : LifecycleService() {
    private val activeIds: MutableList<Int> = mutableListOf()

    private val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)

    data class StopWatches(
        val notificationId: Int,
        val nickname: String,
        val stopId: StopId,
        val routeId: RouteId,
        val duration: Duration
    )


    companion object {
        const val CHANNEL_ID = "live_notification_channel_id"

        const val EXTRA_NICKNAMES = "bus_nicknames"
        const val EXTRA_DURATION = "duration_minutes"
        const val EXTRA_STOP_IDS = "stop_id"
        const val EXTRA_ROUTE_IDS = "route_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TRIP_NICKNAME = "trip_nickname"

        const val ACTION_NAVIGATE_TO_TRIP = "navigate_to_trip"
        const val ACTION_STOP = "action_stop"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            cancelForegroundService(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1))
            return START_NOT_STICKY
        }

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
                { (stopId, routeId), nickname ->
                    StopWatches(
                        0,
                        nickname,
                        StopId(stopId),
                        RouteId(routeId),
                        Duration.ofMinutes(duration)
                    )
                }
                .toList()

            activeIds.add(notificationId)

            startBusPolling(tripNickname, notificationId, watches)
            return START_STICKY
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun cancelForegroundService(notificationId: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
        activeIds.removeIf{ it == notificationId }


        if (activeIds.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startBusPolling(
        nickname: String, notificationId: Int, watches: List<StopWatches>
    ) {
        createActiveNotification(nickname, notificationId)

        lifecycleScope.launch(Dispatchers.IO) {
            pollBuses(watches).collect { result ->
                updateNotification(nickname, notificationId, watches.associateWith {
                    result[Pair(it.stopId, it.routeId)]
                })
            }
        }
    }


    private fun pollBuses(watches: List<StopWatches>): Flow<Map<StopRequest, List<Duration>>> =
        flow {
            while (true) {
                emit(GtfsRealtimeHelper.getBusTimes(watches.map { Pair(it.stopId, it.routeId) }))

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
        nickname: String,
        notificationId: Int,
    ) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(CHANNEL_ID, "Trips", NotificationManager.IMPORTANCE_HIGH).let {
            it.description = "Live trip notifications"
            notificationManager.createNotificationChannel(it)
        }


        // notification for telling the user the app is running
        val notification = getBasicNotification(notificationId)
            .setContentTitle(nickname)
            .build()

        startForeground(notificationId, notification)
    }

    private fun updateNotification(nickname: String, notificationId: Int, stopTimes: Map<StopWatches, List<Duration>?>) {
        val content = stopTimes.map {
            "${it.key.nickname}: ${TextUtils.upcomingBusesString(it.value)}"
        }.joinToString(separator = "\n")

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = getBasicNotification(notificationId)
            .setContentTitle(nickname)
            .setContentText(content)

        notificationManager.notify(notificationId, notification.build())
    }

    private fun getBasicNotification(notificationId: Int): NotificationCompat.Builder {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingStopIntent =  PendingIntent.getBroadcast(
            this,
            notificationId,
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