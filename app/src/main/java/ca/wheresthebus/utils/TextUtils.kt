package ca.wheresthebus.utils

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import ca.wheresthebus.data.UpcomingTime
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

    fun upcomingBusesString(busTimes : List<UpcomingTime>?) : CharSequence {
        if (busTimes.isNullOrEmpty()) {
            return "No upcoming busses"
        }

        val stringBuilder = SpannableStringBuilder()

        busTimes.forEachIndexed { index, it ->
            val durationInMin = it.duration.toMinutes()
            val hour = durationInMin / 60
            val min = durationInMin % 60

            val timeString = when {
                hour >= 1 && min == 0L -> {
                    "$hour hr"
                }
                hour == 0L && min >= 1 -> {
                    "$min min"
                }
                hour >= 1 && min >= 1 -> {
                    "$hour hr $min min"
                }
                else -> "Now"
            }

            // Set bold if isRealtime
            val formatTimeString = if (it.isRealtime) {
                SpannableString(timeString).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, timeString.length, 0)
                }
            } else {
                SpannableString(timeString)
            }
            stringBuilder.append(formatTimeString)

            // Add separators
            if (index != busTimes.lastIndex) {
                stringBuilder.append(", ")
            }
        }

        return stringBuilder
    }
}