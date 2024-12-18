package ca.wheresthebus.utils

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import ca.wheresthebus.R
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

    fun upcomingBusesString(context: Context, busTimes : List<UpcomingTime>?) : CharSequence {
        if (busTimes.isNullOrEmpty()) {
            return "No upcoming buses"
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


            val appendedString = SpannableStringBuilder(timeString).apply {
                if (it.isRealtime) {
                    append("*")
                }
            }

            val formatTimeString = SpannableStringBuilder(appendedString).apply {
                if (it == busTimes.first()) {
                    setSpan(StyleSpan(Typeface.BOLD), 0, appendedString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.md_theme_primary)), 0, appendedString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
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