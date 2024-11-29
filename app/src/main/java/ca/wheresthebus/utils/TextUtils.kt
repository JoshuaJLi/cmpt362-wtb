package ca.wheresthebus.utils

import ca.wheresthebus.data.model.ScheduledTrip
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object TextUtils {

    var shortTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    object ScheduledTripText {
        fun getActivityStatus(trip: ScheduledTrip): String {
            val current = LocalDateTime.now()

            if (trip.isActive(current)) {
                return "Active until ${
                    (trip.getClosestTime(current).toLocalTime().plus(trip.duration).format(
                        shortTimeFormat
                    ))
                }"
            }

            if (trip.isToday(current)) {
                return "Starts at ${
                    shortTimeFormat.format(
                        trip.getClosestTime(current).toLocalTime()
                    )
                }"
            }


            val activeTimes = trip.activeTimes.groupBy { it.time }.map { it ->
                val days = it.value.map { it.day.getDisplayName(TextStyle.SHORT, Locale.CANADA) }
                    .joinToString { it }
                val time = it.key.format(shortTimeFormat)

                return@map "$days at $time"
            }.joinToString { it }

            return "Active on $activeTimes"
        }
    }
}